package com.barbonperformance.courtproof.auth.domain;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokens {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    RefreshToken save(RefreshToken token);

    void revokeFamily(UUID familyId, Instant now);

    void revokeAllForUser(UUID userId, Instant now);
}
