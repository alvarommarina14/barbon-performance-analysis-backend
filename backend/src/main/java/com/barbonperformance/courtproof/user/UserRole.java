package com.barbonperformance.courtproof.user;

public enum UserRole {
    PLAYER,
    COACH,
    ADMIN;

    /** Spring Security expects authorities to carry the {@code ROLE_} prefix. */
    public String authority() {
        return "ROLE_" + name();
    }
}
