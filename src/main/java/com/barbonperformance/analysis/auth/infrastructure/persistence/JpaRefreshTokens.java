package com.barbonperformance.analysis.auth.infrastructure.persistence;

import com.barbonperformance.analysis.auth.domain.RefreshToken;
import com.barbonperformance.analysis.auth.domain.RefreshTokens;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
class JpaRefreshTokens implements RefreshTokens {

    private final RefreshTokenJpaRepository jpa;

    JpaRefreshTokens(RefreshTokenJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return jpa.findByTokenHash(tokenHash);
    }

    @Override
    public RefreshToken save(RefreshToken token) {
        return jpa.save(token);
    }

    @Override
    public void revokeFamily(UUID familyId, Instant now) {
        jpa.revokeFamily(familyId, now);
    }

    @Override
    public void revokeAllForUser(UUID userId, Instant now) {
        jpa.revokeAllForUser(userId, now);
    }
}
