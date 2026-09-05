package com.barbonperformance.analysis.user.infrastructure.persistence;

import com.barbonperformance.analysis.user.domain.CoachProfile;
import com.barbonperformance.analysis.user.domain.CoachProfiles;
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
    public Optional<CoachProfile> findByOrganizationId(UUID organizationId) {
        return jpa.findFirstByOrganizationId(organizationId);
    }

    @Override
    public CoachProfile save(CoachProfile profile) {
        return jpa.save(profile);
    }
}
