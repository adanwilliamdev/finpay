package com.finpay.service;

import com.finpay.entity.AuditLog;
import com.finpay.repository.AuditLogRepository;
import com.finpay.security.SecurityUtil;
import com.finpay.util.RequestUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    /** Actor recorded when the action is not performed by an authenticated user. */
    static final String SYSTEM_USER = "SYSTEM";

    private final AuditLogRepository auditLogRepository;

    /**
     * Persists an audit entry in the caller's transaction.
     * <p>
     * Failures are deliberately NOT swallowed: for a financial API an operation that cannot
     * be audited must not succeed. If the audit row cannot be written the exception
     * propagates and the surrounding business transaction is rolled back.
     * <p>
     * Works both inside and outside an HTTP request (IP / user agent fall back to "unknown").
     */
    @Transactional
    public void logAction(String action, String entityType, String entityId,
                          String oldValue, String newValue, String details) {
        HttpServletRequest request = currentRequest();
        String userId = currentUserId();

        AuditLog auditLog = AuditLog.builder()
                .userId(userId)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .oldValue(oldValue)
                .newValue(newValue != null ? newValue : details)
                .ipAddress(RequestUtil.getClientIp(request))
                .userAgent(RequestUtil.getUserAgent(request))
                .build();

        auditLogRepository.save(auditLog);

        log.debug("Audit log saved: {} - {} - {}", action, entityType, userId);
    }

    /**
     * audit_logs.user_id is a VARCHAR(36) holding the user's ID. The previous implementation
     * stored Authentication#getName() (the e-mail), which both broke the meaning of the column
     * and overflowed it for e-mails longer than 36 characters.
     */
    private String currentUserId() {
        String userId = SecurityUtil.getCurrentUserIdOrNull();
        return userId != null ? userId : SYSTEM_USER;
    }

    private HttpServletRequest currentRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletAttributes) {
            return servletAttributes.getRequest();
        }
        return null;
    }
}
