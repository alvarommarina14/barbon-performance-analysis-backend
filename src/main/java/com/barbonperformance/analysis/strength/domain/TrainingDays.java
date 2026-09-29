package com.barbonperformance.analysis.strength.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TrainingDays {

    List<TrainingDay> findAllByPlayerId(UUID playerProfileId);

    Optional<TrainingDay> findById(UUID trainingDayId);

    void deleteAllByPlayerId(UUID playerProfileId);

    List<TrainingDay> saveAll(List<TrainingDay> trainingDays);
}
