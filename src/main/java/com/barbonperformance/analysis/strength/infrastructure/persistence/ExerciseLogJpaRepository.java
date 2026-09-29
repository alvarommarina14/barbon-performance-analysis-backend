package com.barbonperformance.analysis.strength.infrastructure.persistence;

import com.barbonperformance.analysis.strength.domain.ExerciseLog;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface ExerciseLogJpaRepository extends JpaRepository<ExerciseLog, UUID> {

    List<ExerciseLog> findAllByPlayerIdAndDayExerciseTrainingDayIdAndPlanWeek(UUID playerProfileId,
            UUID trainingDayId, int planWeek);

    Optional<ExerciseLog> findByPlayerIdAndDayExerciseIdAndPlanWeek(UUID playerProfileId, UUID dayExerciseId,
            int planWeek);
}
