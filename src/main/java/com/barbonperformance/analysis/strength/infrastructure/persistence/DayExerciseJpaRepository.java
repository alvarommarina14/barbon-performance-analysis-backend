package com.barbonperformance.analysis.strength.infrastructure.persistence;

import com.barbonperformance.analysis.strength.domain.DayExercise;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface DayExerciseJpaRepository extends JpaRepository<DayExercise, UUID> {

    List<DayExercise> findAllByTrainingDayIdOrderBySortOrder(UUID trainingDayId);

    int countByTrainingDayId(UUID trainingDayId);

    void deleteAllByTrainingDayId(UUID trainingDayId);
}
