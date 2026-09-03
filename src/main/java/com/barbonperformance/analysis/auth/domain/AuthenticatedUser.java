package com.barbonperformance.analysis.auth.domain;

import com.barbonperformance.analysis.user.domain.UserRole;
import java.util.UUID;

public record AuthenticatedUser(UUID id, String email, UserRole role) {
}
