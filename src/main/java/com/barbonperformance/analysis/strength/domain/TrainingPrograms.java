package com.barbonperformance.analysis.strength.domain;

import java.util.Optional;
import java.util.UUID;

public interface TrainingPrograms {

    Optional<TrainingProgram> findByPlayerId(UUID playerProfileId);

    TrainingProgram save(TrainingProgram program);
}
