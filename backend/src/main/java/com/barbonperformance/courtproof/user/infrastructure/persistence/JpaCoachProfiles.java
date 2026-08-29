package com.barbonperformance.courtproof.user.infrastructure.persistence;

import com.barbonperformance.courtproof.user.domain.CoachProfile;
import com.barbonperformance.courtproof.user.domain.CoachProfiles;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
class JpaCoachProfiles implements CoachProfiles {

    private final CoachProfileJpaRepository jpa;

    JpaCoachProfiles(CoachProfileJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<CoachProfile> findByUserId(UUID userId) {
        return jpa.findByUserId(userId);
    }

    @Override
    public CoachProfile save(CoachProfile profile) {
        return jpa.save(profile);
    }
}
