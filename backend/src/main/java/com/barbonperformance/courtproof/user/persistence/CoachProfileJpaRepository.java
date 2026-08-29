package com.barbonperformance.courtproof.user.persistence;

import com.barbonperformance.courtproof.user.domain.CoachProfile;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface CoachProfileJpaRepository extends JpaRepository<CoachProfile, UUID> {

    Optional<CoachProfile> findByUserId(UUID userId);
}
