package com.barbonperformance.analysis.strength.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DayExercises {

    List<DayExercise> findAllByTrainingDayId(UUID trainingDayId);

    Optional<DayExercise> findById(UUID dayExerciseId);

    int countByTrainingDayId(UUID trainingDayId);

    void deleteAll(List<DayExercise> exercises);

    List<DayExercise> saveAll(List<DayExercise> exercises);
}
