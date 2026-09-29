package com.barbonperformance.analysis.strength.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TrainingDays {

    List<TrainingDay> findAllByPlayerId(UUID playerProfileId);

    Optional<TrainingDay> findById(UUID trainingDayId);

    void deleteAll(List<TrainingDay> trainingDays);

    List<TrainingDay> saveAll(List<TrainingDay> trainingDays);
}
