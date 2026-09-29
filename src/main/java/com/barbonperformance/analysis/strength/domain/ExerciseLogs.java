package com.barbonperformance.analysis.strength.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExerciseLogs {

    List<ExerciseLog> findAllForDay(UUID playerProfileId, UUID trainingDayId, int planWeek);

    Optional<ExerciseLog> findOne(UUID playerProfileId, UUID dayExerciseId, int planWeek);

    ExerciseLog save(ExerciseLog log);

    void delete(ExerciseLog log);
}
