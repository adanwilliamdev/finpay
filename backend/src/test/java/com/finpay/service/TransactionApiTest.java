package com.finpay.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finpay.support.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end through the HTTP layer and the real security filter chain: registration,
 * JWT authentication, logout (token revocation), the Idempotency-Key header and error codes.
 */
@AutoConfigureMockMvc
class TransactionApiTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    private record Session(String token, String userId, String walletId) {
    }

    private Session register() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "fullName", "Api Tester",
                "email", "api-" + UUID.randomUUID() + "@example.com",
                "password", "Str0ng@Pass"));
        MvcResult result = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        String token = json.get("token").asText();
        String userId = json.get("userId").asText();

        MvcResult wallet = mockMvc.perform(get("/wallets/user/" + userId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        String walletId = objectMapper.readTree(wallet.getResponse().getContentAsString()).get("id").asText();
        return new Session(token, userId, walletId);
    }

    private MvcResult postJson(Session session, String path, Map<String, Object> payload, String idempotencyKey,
                               int expectedStatus) throws Exception {
        var request = post(path)
                .header("Authorization", "Bearer " + session.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload));
        if (idempotencyKey != null) {
            request.header("Idempotency-Key", idempotencyKey);
        }
        return mockMvc.perform(request).andExpect(status().is(expectedStatus)).andReturn();
    }

    private BigDecimal balance(Session session) throws Exception {
        MvcResult result = mockMvc.perform(get("/wallets/" + session.walletId())
                        .header("Authorization", "Bearer " + session.token()))
                .andExpect(status().isOk()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("balance").decimalValue();
    }

    @Test
    void requestWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/wallets/user/anything")).andExpect(status().isUnauthorized());
    }

    @Test
    void tokenStopsWorkingAfterLogout() throws Exception {
        Session session = register();

        mockMvc.perform(get("/wallets/" + session.walletId()).header("Authorization", "Bearer " + session.token()))
                .andExpect(status().isOk());

        mockMvc.perform(post("/auth/logout").header("Authorization", "Bearer " + session.token()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/wallets/" + session.walletId()).header("Authorization", "Bearer " + session.token()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void depositRetriedWithSameIdempotencyKeyCreditsOnlyOnce() throws Exception {
        Session session = register();
        String key = UUID.randomUUID().toString();
        Map<String, Object> payload = Map.of("walletId", session.walletId(), "amount", new BigDecimal("100.00"));

        MvcResult first = postJson(session, "/transactions/deposit", payload, key, 200);
        MvcResult retry = postJson(session, "/transactions/deposit", payload, key, 200);

        String firstId = objectMapper.readTree(first.getResponse().getContentAsString()).get("id").asText();
        String retryId = objectMapper.readTree(retry.getResponse().getContentAsString()).get("id").asText();
        assertThat(retryId).isEqualTo(firstId);
        assertThat(balance(session)).isEqualByComparingTo("100.00");
    }

    @Test
    void reusingAnIdempotencyKeyWithAnotherPayloadReturns422() throws Exception {
        Session session = register();
        String key = UUID.randomUUID().toString();

        postJson(session, "/transactions/deposit",
                Map.of("walletId", session.walletId(), "amount", new BigDecimal("100.00")), key, 200);
        postJson(session, "/transactions/deposit",
                Map.of("walletId", session.walletId(), "amount", new BigDecimal("250.00")), key, 422);

        assertThat(balance(session)).isEqualByComparingTo("100.00");
    }

    @Test
    void withdrawalEndpointDebitsAndRejectsOverdraft() throws Exception {
        Session session = register();
        postJson(session, "/transactions/deposit",
                Map.of("walletId", session.walletId(), "amount", new BigDecimal("100.00")), null, 200);

        postJson(session, "/transactions/withdraw",
                Map.of("walletId", session.walletId(), "amount", new BigDecimal("40.00")), null, 200);
        assertThat(balance(session)).isEqualByComparingTo("60.00");

        postJson(session, "/transactions/withdraw",
                Map.of("walletId", session.walletId(), "amount", new BigDecimal("60.01")), null, 400);
        assertThat(balance(session)).isEqualByComparingTo("60.00");
    }

    @Test
    void anotherUsersWalletIsForbidden() throws Exception {
        Session owner = register();
        Session stranger = register();

        postJson(stranger, "/transactions/withdraw",
                Map.of("walletId", owner.walletId(), "amount", new BigDecimal("1.00")), null, 403);
    }
}
