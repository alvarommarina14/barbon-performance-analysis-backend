package com.barbonperformance.analysis.strength.infrastructure.persistence;

import com.barbonperformance.analysis.strength.domain.TrainingProgram;
import com.barbonperformance.analysis.strength.domain.TrainingPrograms;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
class JpaTrainingPrograms implements TrainingPrograms {

    private final TrainingProgramJpaRepository jpa;

    JpaTrainingPrograms(TrainingProgramJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<TrainingProgram> findByPlayerId(UUID playerProfileId) {
        return jpa.findByPlayerId(playerProfileId);
    }

    @Override
    public TrainingProgram save(TrainingProgram program) {
        return jpa.save(program);
    }
}
