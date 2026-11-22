package com.finpay.security.ratelimit;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RateLimiterTest {

    private static final class MutableClock extends Clock {
        long millis = 1_000_000L;

        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return Instant.ofEpochMilli(millis); }
        @Override public long millis() { return millis; }
    }

    @Test
    void blocksAfterLimitAndReportsRetryAfter() {
        RateLimiter limiter = new RateLimiter(3, Duration.ofSeconds(60), new MutableClock());

        for (int i = 0; i < 3; i++) {
            assertThat(limiter.tryAcquire("ip").allowed()).isTrue();
        }
        RateLimiter.Decision blocked = limiter.tryAcquire("ip");

        assertThat(blocked.allowed()).isFalse();
        assertThat(blocked.retryAfterSeconds()).isEqualTo(60);
    }

    @Test
    void keysAreIndependent() {
        RateLimiter limiter = new RateLimiter(1, Duration.ofSeconds(60), new MutableClock());

        assertThat(limiter.tryAcquire("a").allowed()).isTrue();
        assertThat(limiter.tryAcquire("a").allowed()).isFalse();
        assertThat(limiter.tryAcquire("b").allowed()).isTrue();
    }

    @Test
    void windowResetsAndRetryAfterRoundsUp() {
        MutableClock clock = new MutableClock();
        RateLimiter limiter = new RateLimiter(1, Duration.ofSeconds(60), clock);
        limiter.tryAcquire("ip");

        clock.millis += 59_500;
        RateLimiter.Decision almost = limiter.tryAcquire("ip");
        assertThat(almost.allowed()).isFalse();
        assertThat(almost.retryAfterSeconds()).isEqualTo(1);

        clock.millis += 500;
        assertThat(limiter.tryAcquire("ip").allowed()).isTrue();
    }

    @Test
    void resetAndPurgeFreeMemory() {
        MutableClock clock = new MutableClock();
        RateLimiter limiter = new RateLimiter(1, Duration.ofSeconds(60), clock);
        limiter.tryAcquire("a");
        limiter.tryAcquire("b");

        limiter.reset("a");
        assertThat(limiter.size()).isEqualTo(1);

        clock.millis += 61_000;
        assertThat(limiter.purgeExpired()).isEqualTo(1);
        assertThat(limiter.size()).isZero();
    }

    @Test
    void rejectsInvalidConfiguration() {
        assertThatThrownBy(() -> new RateLimiter(0, Duration.ofSeconds(1))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RateLimiter(1, Duration.ZERO)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void concurrentCallersNeverExceedTheLimit() throws Exception {
        RateLimiter limiter = new RateLimiter(50, Duration.ofMinutes(1));
        AtomicInteger allowed = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(16);
        List<Future<?>> futures = new ArrayList<>();
        for (int t = 0; t < 16; t++) {
            futures.add(pool.submit(() -> {
                for (int i = 0; i < 20; i++) {
                    if (limiter.tryAcquire("shared").allowed()) {
                        allowed.incrementAndGet();
                    }
                }
            }));
        }
        for (Future<?> f : futures) {
            f.get(10, TimeUnit.SECONDS);
        }
        pool.shutdownNow();

        assertThat(allowed.get()).isEqualTo(50);
    }
}
