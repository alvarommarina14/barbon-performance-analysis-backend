package com.barbonperformance.courtproof.user;

public enum UserRole {
    PLAYER,
    COACH,
    ADMIN;

    public String authority() {
        return "ROLE_" + name();
    }
}
