package com.barbonperformance.courtproof.auth.domain;

import com.barbonperformance.courtproof.user.domain.UserRole;
import java.util.UUID;

public record AuthenticatedUser(UUID id, String email, UserRole role) {
}
