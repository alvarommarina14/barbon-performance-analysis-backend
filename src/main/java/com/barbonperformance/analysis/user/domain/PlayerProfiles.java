package com.barbonperformance.analysis.user.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlayerProfiles {

    Optional<PlayerProfile> findByUserId(UUID userId);

    List<PlayerProfile> findAllByCoachId(UUID coachProfileId);

    PlayerProfile save(PlayerProfile profile);
}
