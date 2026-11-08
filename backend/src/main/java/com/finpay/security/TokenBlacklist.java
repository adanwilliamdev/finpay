package com.finpay.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side list of revoked tokens (by {@code jti}), filled on logout.
 * <p>
 * An entry only has to live until the token would have expired anyway, so the map is purged
 * periodically and never grows without bound.
 * <p>
 * Limitation (documented in the README): the list is in memory, so it is per instance and is
 * lost on restart. With more than one replica, move it to a shared store such as Redis.
 */
@Component
@Slf4j
public class TokenBlacklist {

    private final Map<String, Instant> revoked = new ConcurrentHashMap<>();

    public void revoke(String tokenId, Instant expiresAt) {
        if (tokenId == null || expiresAt == null) {
            return;
        }
        revoked.put(tokenId, expiresAt);
    }

    /** Tokens without a jti (issued before revocation existed) are never considered revoked. */
    public boolean isRevoked(String tokenId) {
        return tokenId != null && revoked.containsKey(tokenId);
    }

    public int size() {
        return revoked.size();
    }

    @Scheduled(fixedDelay = 600_000L)
    public void purgeExpired() {
        Instant now = Instant.now();
        int before = revoked.size();
        revoked.values().removeIf(expiry -> expiry.isBefore(now));
        int removed = before - revoked.size();
        if (removed > 0) {
            log.debug("Purged {} expired revoked tokens", removed);
        }
    }
}
