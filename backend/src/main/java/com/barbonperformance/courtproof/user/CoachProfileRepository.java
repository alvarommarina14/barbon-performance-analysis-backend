package com.barbonperformance.courtproof.user;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CoachProfileRepository extends JpaRepository<CoachProfile, UUID> {

    Optional<CoachProfile> findByUserId(UUID userId);
}
