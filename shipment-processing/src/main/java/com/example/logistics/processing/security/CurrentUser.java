package com.example.logistics.processing.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public record CurrentUser(String id, boolean admin) {

    public CurrentUser {
        if (id == null || id.isBlank() || id.length() > 255) {
            throw new AccessDeniedException("A valid JWT subject is required");
        }
    }

    public static CurrentUser from(JwtAuthenticationToken authentication) {
        boolean admin = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        return new CurrentUser(authentication.getToken().getSubject(), admin);
    }
}
