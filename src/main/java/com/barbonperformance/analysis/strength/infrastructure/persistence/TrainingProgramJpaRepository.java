package com.barbonperformance.analysis.strength.infrastructure.persistence;

import com.barbonperformance.analysis.strength.domain.TrainingProgram;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface TrainingProgramJpaRepository extends JpaRepository<TrainingProgram, UUID> {

    Optional<TrainingProgram> findByPlayerId(UUID playerProfileId);
}
