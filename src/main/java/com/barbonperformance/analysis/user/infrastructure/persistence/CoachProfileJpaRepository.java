package com.barbonperformance.analysis.user.infrastructure.persistence;

import com.barbonperformance.analysis.user.domain.CoachProfile;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface CoachProfileJpaRepository extends JpaRepository<CoachProfile, UUID> {

    Optional<CoachProfile> findByUserId(UUID userId);
}
