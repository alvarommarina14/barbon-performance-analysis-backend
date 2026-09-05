package com.barbonperformance.analysis.user.domain;

import java.util.Optional;
import java.util.UUID;

public interface CoachProfiles {

    Optional<CoachProfile> findByUserId(UUID userId);

    Optional<CoachProfile> findByOrganizationId(UUID organizationId);

    CoachProfile save(CoachProfile profile);
}
