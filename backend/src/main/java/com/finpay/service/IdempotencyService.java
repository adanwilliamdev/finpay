package com.finpay.service;

import com.finpay.entity.IdempotencyKey;
import com.finpay.exception.IdempotencyConflictException;
import com.finpay.repository.IdempotencyKeyRepository;
import com.finpay.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Support for the {@code Idempotency-Key} request header on money-moving endpoints.
 * <p>
 * Contract: the first request with a given key (per user) runs normally and its outcome is
 * stored atomically with the money movement. A retry with the same key and the same payload
 * returns the stored outcome without touching any balance. The same key with a different
 * payload is rejected with 422. Requests rejected by a business rule are not stored, so the
 * client may retry them with the same key.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IdempotencyService {

    static final int MAX_KEY_LENGTH = 100;

    private final IdempotencyKeyRepository repository;

    @Value("${finpay.idempotency.retention-hours:24}")
    private long retentionHours;

    /** Returns the trimmed key, or null when the client did not send one. */
    public String normalize(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        String trimmed = key.trim();
        if (trimmed.length() > MAX_KEY_LENGTH) {
            throw new IllegalArgumentException(
                    "Idempotency-Key must not exceed " + MAX_KEY_LENGTH + " characters");
        }
        return trimmed;
    }

    /**
     * @return the id of the transaction produced by the first execution, if this key was
     * already used by the current user with the same payload.
     * @throws IdempotencyConflictException if it was used with a different payload
     */
    @Transactional(readOnly = true)
    public Optional<String> findCompletedTransactionId(String key, String requestHash) {
        return repository.findByUserIdAndIdempotencyKey(SecurityUtil.getCurrentUserId(), key)
                .map(existing -> {
                    if (!existing.getRequestHash().equals(requestHash)) {
                        throw new IdempotencyConflictException(
                                "Idempotency-Key was already used with a different request");
                    }
                    return existing.getTransactionId();
                });
    }

    /**
     * Stores the key. Must be called inside the transaction that moves the money, so both
     * commit or roll back together. Flushes immediately so a concurrent duplicate fails here
     * (unique constraint) and rolls the duplicate movement back.
     */
    public void remember(String key, String requestHash, String transactionId) {
        repository.saveAndFlush(IdempotencyKey.builder()
                .userId(SecurityUtil.getCurrentUserId())
                .idempotencyKey(key)
                .requestHash(requestHash)
                .transactionId(transactionId)
                .build());
    }

    public static String hash(String... parts) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(String.join("\u001f", parts).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void purgeExpired() {
        int removed = repository.deleteOlderThan(LocalDateTime.now().minusHours(retentionHours));
        if (removed > 0) {
            log.info("Purged {} expired idempotency keys", removed);
        }
    }
}
