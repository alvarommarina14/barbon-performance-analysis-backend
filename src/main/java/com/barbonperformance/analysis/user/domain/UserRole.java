package com.barbonperformance.analysis.user.domain;

public enum UserRole {
    PLAYER,
    COACH,
    ADMIN;

    public String authority() {
        return "ROLE_" + name();
    }
}
