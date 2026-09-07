package com.finpay.security;

import com.finpay.entity.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Small helper to fetch the currently authenticated user from the security context.
 * Centralizing this avoids every service/controller re-implementing (and possibly
 * getting wrong) the cast from Authentication -> our User entity.
 */
public final class SecurityUtil {

    private SecurityUtil() {
    }

    public static User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User)) {
            throw new org.springframework.security.access.AccessDeniedException("No authenticated user found");
        }
        return (User) authentication.getPrincipal();
    }

    public static String getCurrentUserId() {
        return getCurrentUser().getId();
    }

    public static boolean isAdmin() {
        return getCurrentUser().getRole() == User.UserRole.ADMIN;
    }

    /**
     * Throws AccessDeniedException unless the current user IS the given resource owner
     * (by user id) or is an ADMIN.
     */
    public static void checkOwnerOrAdmin(String resourceOwnerUserId) {
        User current = getCurrentUser();
        boolean isOwner = current.getId().equals(resourceOwnerUserId);
        boolean isAdmin = current.getRole() == User.UserRole.ADMIN;
        if (!isOwner && !isAdmin) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "You don't have permission to access this resource");
        }
    }
}
