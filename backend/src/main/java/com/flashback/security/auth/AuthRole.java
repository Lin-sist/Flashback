package com.flashback.security.auth;

public enum AuthRole {
    USER,
    ADMIN;

    public static AuthRole from(String value) {
        if (value == null) {
            throw new IllegalArgumentException("role is required");
        }
        return AuthRole.valueOf(value);
    }
}
