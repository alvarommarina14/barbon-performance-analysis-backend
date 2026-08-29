package com.barbonperformance.courtproof.auth;

import com.barbonperformance.courtproof.user.UserRole;
import java.util.UUID;

public record AuthenticatedUser(UUID id, String email, UserRole role) {
}
