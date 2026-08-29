package com.barbonperformance.courtproof.user.domain;

public enum UserRole {
    PLAYER,
    COACH,
    ADMIN;

    public String authority() {
        return "ROLE_" + name();
    }
}
