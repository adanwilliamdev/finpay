package com.finpay.security.ratelimit;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/** Pure unit tests (no Spring context, no Docker). */
class RateLimitFilterTest {

    private RateLimitFilter filter(boolean enabled, boolean trustForwardedFor, int loginMax) {
        return new RateLimitFilter(enabled, trustForwardedFor, loginMax, 60, 5, 3600);
    }

    private MockHttpServletRequest post(String servletPath, String remoteAddr) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api" + servletPath);
        request.setServletPath(servletPath);
        request.setRemoteAddr(remoteAddr);
        return request;
    }

    private MockHttpServletResponse run(RateLimitFilter filter, MockHttpServletRequest request) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }

    @Test
    void returns429WithRetryAfterOnceLimitIsExceeded() throws Exception {
        RateLimitFilter filter = filter(true, false, 2);

        assertThat(run(filter, post("/auth/login", "10.0.0.1")).getStatus()).isEqualTo(200);
        assertThat(run(filter, post("/auth/login", "10.0.0.1")).getStatus()).isEqualTo(200);
        MockHttpServletResponse blocked = run(filter, post("/auth/login", "10.0.0.1"));

        assertThat(blocked.getStatus()).isEqualTo(429);
        assertThat(blocked.getHeader("Retry-After")).isNotNull();
        assertThat(blocked.getContentAsString()).contains("\"status\":429");
    }

    @Test
    void otherClientsAreNotAffected() throws Exception {
        RateLimitFilter filter = filter(true, false, 1);

        run(filter, post("/auth/login", "10.0.0.1"));

        assertThat(run(filter, post("/auth/login", "10.0.0.1")).getStatus()).isEqualTo(429);
        assertThat(run(filter, post("/auth/login", "10.0.0.2")).getStatus()).isEqualTo(200);
    }

    @Test
    void loginAndRegisterHaveSeparateBuckets() throws Exception {
        RateLimitFilter filter = filter(true, false, 1);

        run(filter, post("/auth/login", "10.0.0.1"));

        assertThat(run(filter, post("/auth/login", "10.0.0.1")).getStatus()).isEqualTo(429);
        assertThat(run(filter, post("/auth/register", "10.0.0.1")).getStatus()).isEqualTo(200);
    }

    @Test
    void otherRoutesAndMethodsAreNeverThrottled() throws Exception {
        RateLimitFilter filter = filter(true, false, 1);

        for (int i = 0; i < 20; i++) {
            assertThat(run(filter, post("/transactions/transfer", "10.0.0.1")).getStatus()).isEqualTo(200);
            MockHttpServletRequest get = new MockHttpServletRequest("GET", "/api/auth/login");
            get.setServletPath("/auth/login");
            assertThat(run(filter, get).getStatus()).isEqualTo(200);
        }
    }

    @Test
    void disabledFilterLetsEverythingThrough() throws Exception {
        RateLimitFilter filter = filter(false, false, 1);

        for (int i = 0; i < 10; i++) {
            assertThat(run(filter, post("/auth/login", "10.0.0.1")).getStatus()).isEqualTo(200);
        }
    }

    @Test
    void forwardedForIsIgnoredUnlessTrusted() throws Exception {
        RateLimitFilter untrusting = filter(true, false, 1);

        MockHttpServletRequest first = post("/auth/login", "10.0.0.1");
        first.addHeader("X-Forwarded-For", "1.1.1.1");
        MockHttpServletRequest second = post("/auth/login", "10.0.0.1");
        second.addHeader("X-Forwarded-For", "2.2.2.2");

        run(untrusting, first);
        // A forged header must not buy a fresh bucket.
        assertThat(run(untrusting, second).getStatus()).isEqualTo(429);
    }

    @Test
    void trustedForwardedForUsesTheLastHop() throws Exception {
        RateLimitFilter filter = filter(true, true, 1);

        MockHttpServletRequest first = post("/auth/login", "172.18.0.5");
        first.addHeader("X-Forwarded-For", "forged-1, 203.0.113.7");
        MockHttpServletRequest sameClientDifferentForgery = post("/auth/login", "172.18.0.5");
        sameClientDifferentForgery.addHeader("X-Forwarded-For", "forged-2, 203.0.113.7");
        MockHttpServletRequest otherClient = post("/auth/login", "172.18.0.5");
        otherClient.addHeader("X-Forwarded-For", "forged-1, 198.51.100.9");

        run(filter, first);
        assertThat(run(filter, sameClientDifferentForgery).getStatus()).isEqualTo(429);
        assertThat(run(filter, otherClient).getStatus()).isEqualTo(200);
    }
}
