package com.barbonperformance.analysis.strength.domain;

import java.util.List;
import java.util.UUID;

public interface DayExercises {

    List<DayExercise> findAllByTrainingDayId(UUID trainingDayId);

    int countByTrainingDayId(UUID trainingDayId);

    void deleteAllByTrainingDayId(UUID trainingDayId);

    List<DayExercise> saveAll(List<DayExercise> exercises);
}
