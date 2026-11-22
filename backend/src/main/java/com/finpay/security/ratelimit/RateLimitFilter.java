package com.finpay.security.ratelimit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Throttles the unauthenticated entry points (login and registration) per client IP.
 * Over the limit the request never reaches the controller: 429 + {@code Retry-After}.
 * <p>
 * Registered inside the Spring Security chain right after the CORS filter (see SecurityConfig), so a
 * 429 still carries the CORS headers and the browser lets the frontend read it.
 * <p>
 * Client IP: by default {@code getRemoteAddr()}. {@code X-Forwarded-For} is client-controlled,
 * so it is only used when {@code finpay.rate-limit.trust-forwarded-for=true} (i.e. the app sits
 * behind a proxy that appends the real peer address). In that case the LAST hop is used,
 * because that is the one added by our own proxy; the first hops can be forged by the caller.
 */
@Component
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private final boolean enabled;
    private final boolean trustForwardedFor;
    private final RateLimiter loginLimiter;
    private final RateLimiter registerLimiter;

    public RateLimitFilter(
            @Value("${finpay.rate-limit.enabled:true}") boolean enabled,
            @Value("${finpay.rate-limit.trust-forwarded-for:false}") boolean trustForwardedFor,
            @Value("${finpay.rate-limit.login.max-requests:10}") int loginMax,
            @Value("${finpay.rate-limit.login.window-seconds:60}") int loginWindowSeconds,
            @Value("${finpay.rate-limit.register.max-requests:5}") int registerMax,
            @Value("${finpay.rate-limit.register.window-seconds:3600}") int registerWindowSeconds) {
        this.enabled = enabled;
        this.trustForwardedFor = trustForwardedFor;
        this.loginLimiter = new RateLimiter(loginMax, Duration.ofSeconds(loginWindowSeconds));
        this.registerLimiter = new RateLimiter(registerMax, Duration.ofSeconds(registerWindowSeconds));
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!enabled || !HttpMethod.POST.matches(request.getMethod())) {
            return true;
        }
        String path = request.getServletPath();
        return !("/auth/login".equals(path) || "/auth/register".equals(path));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        RateLimiter limiter = "/auth/login".equals(request.getServletPath()) ? loginLimiter : registerLimiter;
        String clientIp = resolveClientIp(request);

        RateLimiter.Decision decision = limiter.tryAcquire(clientIp);
        if (decision.allowed()) {
            chain.doFilter(request, response);
            return;
        }

        log.warn("Rate limit exceeded on {} for {}", request.getServletPath(), clientIp);
        response.setStatus(429);
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(decision.retryAfterSeconds()));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        // Same shape as GlobalExceptionHandler.ErrorResponse so the frontend reads it the same way.
        response.getWriter().write(String.format(
                "{\"timestamp\":\"%s\",\"status\":429,\"error\":\"Too Many Requests\","
                        + "\"message\":\"Too many attempts. Try again in %d seconds.\",\"path\":\"%s\"}",
                LocalDateTime.now(), decision.retryAfterSeconds(), request.getServletPath()));
    }

    String resolveClientIp(HttpServletRequest request) {
        if (trustForwardedFor) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                String[] hops = forwarded.split(",");
                String last = hops[hops.length - 1].trim();
                if (!last.isEmpty()) {
                    return last;
                }
            }
        }
        String remote = request.getRemoteAddr();
        return remote == null ? "unknown" : remote;
    }

    @Scheduled(fixedDelay = 300_000L)
    public void purgeExpired() {
        int removed = loginLimiter.purgeExpired() + registerLimiter.purgeExpired();
        if (removed > 0) {
            log.debug("Purged {} expired rate-limit windows", removed);
        }
    }
}
