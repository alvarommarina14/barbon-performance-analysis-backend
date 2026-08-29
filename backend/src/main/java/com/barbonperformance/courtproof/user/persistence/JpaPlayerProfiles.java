package com.barbonperformance.courtproof.user.persistence;

import com.barbonperformance.courtproof.user.domain.PlayerProfile;
import com.barbonperformance.courtproof.user.domain.PlayerProfiles;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
class JpaPlayerProfiles implements PlayerProfiles {

    private final PlayerProfileJpaRepository jpa;

    JpaPlayerProfiles(PlayerProfileJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<PlayerProfile> findByUserId(UUID userId) {
        return jpa.findByUserId(userId);
    }

    @Override
    public List<PlayerProfile> findAllByCoachId(UUID coachProfileId) {
        return jpa.findAllByCoachId(coachProfileId);
    }

    @Override
    public PlayerProfile save(PlayerProfile profile) {
        return jpa.save(profile);
    }
}
