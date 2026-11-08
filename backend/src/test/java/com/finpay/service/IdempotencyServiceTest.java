package com.finpay.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IdempotencyServiceTest {

    private final IdempotencyService service = new IdempotencyService(null);

    @Test
    void blankKeyMeansNoIdempotency() {
        assertThat(service.normalize(null)).isNull();
        assertThat(service.normalize("   ")).isNull();
    }

    @Test
    void keyIsTrimmed() {
        assertThat(service.normalize("  abc-123  ")).isEqualTo("abc-123");
    }

    @Test
    void keyLongerThanTheColumnIsRejected() {
        assertThat(service.normalize("k".repeat(100))).hasSize(100);
        assertThatThrownBy(() -> service.normalize("k".repeat(101)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void hashIsDeterministicAndSensitiveToEveryPart() {
        String base = IdempotencyService.hash("TRANSFER", "w1", "w2", "10", "");
        assertThat(IdempotencyService.hash("TRANSFER", "w1", "w2", "10", "")).isEqualTo(base);
        assertThat(IdempotencyService.hash("TRANSFER", "w1", "w2", "11", "")).isNotEqualTo(base);
        assertThat(IdempotencyService.hash("TRANSFER", "w1", "w3", "10", "")).isNotEqualTo(base);
        assertThat(IdempotencyService.hash("DEPOSIT", "w1", "w2", "10", "")).isNotEqualTo(base);
        assertThat(base).hasSize(64);
    }

    @Test
    void hashSeparatesPartsSoShiftedValuesDoNotCollide() {
        assertThat(IdempotencyService.hash("ab", "c")).isNotEqualTo(IdempotencyService.hash("a", "bc"));
    }
}
