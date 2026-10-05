package com.barbonperformance.analysis.strength.infrastructure.persistence;

import com.barbonperformance.analysis.strength.domain.ExerciseLog;
import com.barbonperformance.analysis.strength.domain.ExerciseLogs;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
class JpaExerciseLogs implements ExerciseLogs {

    private final ExerciseLogJpaRepository jpa;

    JpaExerciseLogs(ExerciseLogJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public List<ExerciseLog> findAllForDay(UUID playerProfileId, UUID trainingDayId, int planWeek) {
        return jpa.findAllByPlayerIdAndDayExerciseTrainingDayIdAndPlanWeek(playerProfileId, trainingDayId, planWeek);
    }

    @Override
    public Optional<ExerciseLog> findOne(UUID playerProfileId, UUID dayExerciseId, int planWeek) {
        return jpa.findByPlayerIdAndDayExerciseIdAndPlanWeek(playerProfileId, dayExerciseId, planWeek);
    }

    @Override
    public ExerciseLog save(ExerciseLog log) {
        return jpa.save(log);
    }

    @Override
    public void delete(ExerciseLog log) {
        jpa.delete(log);
    }
}
