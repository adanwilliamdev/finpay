package com.finpay.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Extracts client metadata (IP / user agent) from an HTTP request in a way that always
 * yields a value that fits the NOT NULL columns of transactions and audit_logs.
 * <p>
 * Both helpers accept {@code null} so callers running outside an HTTP request
 * (seed jobs, schedulers, tests) don't need special handling.
 */
public final class RequestUtil {

    /** Column size of ip_address (fits an IPv6 literal). */
    private static final int MAX_IP_LENGTH = 45;
    private static final String UNKNOWN = "unknown";

    private RequestUtil() {
    }

    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return UNKNOWN;
        }
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank()) {
            // X-Forwarded-For may be a comma separated chain "client, proxy1, proxy2";
            // the whole chain does not fit in the column, keep the first hop.
            ip = ip.split(",")[0].trim();
        } else {
            ip = request.getRemoteAddr();
        }
        if (ip == null || ip.isBlank()) {
            return UNKNOWN;
        }
        return ip.length() > MAX_IP_LENGTH ? ip.substring(0, MAX_IP_LENGTH) : ip;
    }

    public static String getUserAgent(HttpServletRequest request) {
        if (request == null) {
            return UNKNOWN;
        }
        String userAgent = request.getHeader("User-Agent");
        return (userAgent == null || userAgent.isBlank()) ? UNKNOWN : userAgent;
    }
}
