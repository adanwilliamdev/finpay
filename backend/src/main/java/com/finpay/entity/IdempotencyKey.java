package com.finpay.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Remembers the outcome of a request sent with an {@code Idempotency-Key} header, so a retry
 * (network timeout, double click) returns the original result instead of moving money twice.
 * Unique per (user, key).
 */
@Entity
@Table(name = "idempotency_keys")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class IdempotencyKey {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, updatable = false)
    private String userId;

    @Column(nullable = false, updatable = false)
    private String idempotencyKey;

    /** SHA-256 of the request payload: the same key with another payload is a client bug. */
    @Column(nullable = false, updatable = false)
    private String requestHash;

    /** Id of the ledger entry produced by the first execution. */
    @Column(nullable = false, updatable = false)
    private String transactionId;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
