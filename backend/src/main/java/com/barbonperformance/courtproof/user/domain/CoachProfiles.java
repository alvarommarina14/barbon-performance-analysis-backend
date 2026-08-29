package com.barbonperformance.courtproof.user.domain;

import java.util.Optional;
import java.util.UUID;

public interface CoachProfiles {

    Optional<CoachProfile> findByUserId(UUID userId);

    CoachProfile save(CoachProfile profile);
}
