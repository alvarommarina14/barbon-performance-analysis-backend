package com.barbonperformance.courtproof.auth;

import com.barbonperformance.courtproof.user.UserRole;
import java.util.UUID;

/**
 * What the access token carries. Injected into controllers with
 * {@code @AuthenticationPrincipal} so they never have to parse a token themselves.
 */
public record AuthenticatedUser(UUID id, String email, UserRole role) {
}
