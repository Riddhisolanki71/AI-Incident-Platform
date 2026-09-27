package com.aiincident.security;

import org.springframework.security.core.context.SecurityContextHolder;

/** Small helper to read the currently authenticated user's email from the security context. */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static String currentUserEmail() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return null;
        }
        return (String) auth.getPrincipal();
    }
}
