package com.finpay.security;

import com.finpay.entity.User;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenProviderTest {

    private static final String STRONG_SECRET = "x".repeat(64);

    private JwtTokenProvider providerWith(String secret) {
        JwtTokenProvider provider = new JwtTokenProvider();
        ReflectionTestUtils.setField(provider, "jwtSecret", secret);
        ReflectionTestUtils.setField(provider, "jwtExpiration", 60_000L);
        return provider;
    }

    private User user() {
        return User.builder().id("u1").email("jwt@example.com").fullName("Jwt Tester")
                .role(User.UserRole.USER).password("x").build();
    }

    @Test
    void startupFailsWhenSecretIsMissing() {
        assertThatThrownBy(() -> providerWith(null).validateConfiguration())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
        assertThatThrownBy(() -> providerWith("  ").validateConfiguration())
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void startupFailsWhenSecretIsTooShortForHs512() {
        assertThatThrownBy(() -> providerWith("x".repeat(63)).validateConfiguration())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 64");
    }

    @Test
    void strongSecretIsAccepted() {
        providerWith(STRONG_SECRET).validateConfiguration();
    }

    @Test
    void generatedTokenIsValidAndCarriesClaimsAndUniqueId() {
        JwtTokenProvider provider = providerWith(STRONG_SECRET);

        String first = provider.generateToken(user());
        String second = provider.generateToken(user());

        assertThat(provider.validateToken(first)).isTrue();
        Claims claims = provider.parseClaims(first);
        assertThat(claims.getSubject()).isEqualTo("jwt@example.com");
        assertThat(claims.get("role", String.class)).isEqualTo("USER");
        assertThat(claims.getId()).isNotBlank();
        assertThat(provider.parseClaims(second).getId()).isNotEqualTo(claims.getId());
    }

    @Test
    void tamperedTokenIsRejected() {
        JwtTokenProvider provider = providerWith(STRONG_SECRET);
        String token = provider.generateToken(user());
        String tampered = token.substring(0, token.length() - 4) + "AAAA";

        assertThat(provider.validateToken(tampered)).isFalse();
    }

    @Test
    void tokenSignedWithAnotherSecretIsRejected() {
        String token = providerWith(STRONG_SECRET).generateToken(user());
        assertThat(providerWith("y".repeat(64)).validateToken(token)).isFalse();
    }

    @Test
    void expiredTokenIsRejected() {
        JwtTokenProvider provider = providerWith(STRONG_SECRET);
        ReflectionTestUtils.setField(provider, "jwtExpiration", -1_000L);
        assertThat(provider.validateToken(provider.generateToken(user()))).isFalse();
    }
}
