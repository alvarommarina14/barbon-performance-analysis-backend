package com.barbonperformance.analysis.strength.infrastructure.persistence;

import com.barbonperformance.analysis.strength.domain.DayExercise;
import com.barbonperformance.analysis.strength.domain.DayExercises;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
class JpaDayExercises implements DayExercises {

    private final DayExerciseJpaRepository jpa;

    JpaDayExercises(DayExerciseJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public List<DayExercise> findAllByTrainingDayId(UUID trainingDayId) {
        return jpa.findAllByTrainingDayIdOrderBySortOrder(trainingDayId);
    }

    @Override
    public Optional<DayExercise> findById(UUID dayExerciseId) {
        return jpa.findById(dayExerciseId);
    }

    @Override
    public int countByTrainingDayId(UUID trainingDayId) {
        return jpa.countByTrainingDayId(trainingDayId);
    }

    @Override
    public void deleteAll(List<DayExercise> exercises) {
        jpa.deleteAll(exercises);
    }

    @Override
    public List<DayExercise> saveAll(List<DayExercise> exercises) {
        return jpa.saveAll(exercises);
    }
}
