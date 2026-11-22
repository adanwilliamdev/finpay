package com.finpay.security.ratelimit;

import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fixed-window rate limiter keyed by an arbitrary string (IP, e-mail, ...).
 * <p>
 * Deliberately free of Spring/servlet types so it can be unit-tested in isolation and
 * swapped for a shared store (Redis) later without touching the filter.
 * <p>
 * Limitation: state is in memory, so limits apply per instance and reset on restart.
 */
public class RateLimiter {

    /** Outcome of an attempt: whether it may proceed and, if not, how long to wait. */
    public record Decision(boolean allowed, long retryAfterSeconds) {
        static final Decision ALLOWED = new Decision(true, 0);
    }

    private static final class Window {
        final long startMillis;
        int count;

        Window(long startMillis) {
            this.startMillis = startMillis;
        }
    }

    private final int maxRequests;
    private final long windowMillis;
    private final Clock clock;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    public RateLimiter(int maxRequests, Duration window, Clock clock) {
        if (maxRequests < 1) {
            throw new IllegalArgumentException("maxRequests must be >= 1");
        }
        if (window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException("window must be positive");
        }
        this.maxRequests = maxRequests;
        this.windowMillis = window.toMillis();
        this.clock = clock;
    }

    public RateLimiter(int maxRequests, Duration window) {
        this(maxRequests, window, Clock.systemUTC());
    }

    /** Counts one attempt for {@code key} and says whether it is within the limit. */
    public Decision tryAcquire(String key) {
        long now = clock.millis();
        Decision[] result = new Decision[1];

        windows.compute(key, (k, current) -> {
            Window w = current;
            if (w == null || now - w.startMillis >= windowMillis) {
                w = new Window(now);
            }
            if (w.count < maxRequests) {
                w.count++;
                result[0] = Decision.ALLOWED;
            } else {
                long remainingMillis = windowMillis - (now - w.startMillis);
                result[0] = new Decision(false, Math.max(1, (remainingMillis + 999) / 1000));
            }
            return w;
        });

        return result[0];
    }

    /** Forgets the key (e.g. after a successful login). */
    public void reset(String key) {
        windows.remove(key);
    }

    /** Drops windows that have already elapsed; call periodically so the map stays small. */
    public int purgeExpired() {
        long now = clock.millis();
        int before = windows.size();
        windows.entrySet().removeIf(e -> now - e.getValue().startMillis >= windowMillis);
        return before - windows.size();
    }

    public int size() {
        return windows.size();
    }
}
