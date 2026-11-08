package com.finpay.security;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class TokenBlacklistTest {

    private final TokenBlacklist blacklist = new TokenBlacklist();

    @Test
    void revokedTokenIsReportedAsRevoked() {
        blacklist.revoke("jti-1", Instant.now().plusSeconds(60));
        assertThat(blacklist.isRevoked("jti-1")).isTrue();
        assertThat(blacklist.isRevoked("jti-2")).isFalse();
    }

    @Test
    void tokensWithoutIdAreNeverRevoked() {
        blacklist.revoke(null, Instant.now().plusSeconds(60));
        assertThat(blacklist.isRevoked(null)).isFalse();
        assertThat(blacklist.size()).isZero();
    }

    @Test
    void purgeRemovesOnlyExpiredEntries() {
        blacklist.revoke("expired", Instant.now().minusSeconds(1));
        blacklist.revoke("active", Instant.now().plusSeconds(60));

        blacklist.purgeExpired();

        assertThat(blacklist.isRevoked("expired")).isFalse();
        assertThat(blacklist.isRevoked("active")).isTrue();
    }
}
