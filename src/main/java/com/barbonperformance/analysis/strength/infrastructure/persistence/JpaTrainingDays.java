package com.barbonperformance.analysis.strength.infrastructure.persistence;

import com.barbonperformance.analysis.strength.domain.TrainingDay;
import com.barbonperformance.analysis.strength.domain.TrainingDays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
class JpaTrainingDays implements TrainingDays {

    private final TrainingDayJpaRepository jpa;

    JpaTrainingDays(TrainingDayJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public List<TrainingDay> findAllByPlayerId(UUID playerProfileId) {
        return jpa.findAllByPlayerIdOrderBySortOrder(playerProfileId);
    }

    @Override
    public List<TrainingDay> findAllByTemplateId(UUID templateId) {
        return jpa.findAllByTemplateIdOrderBySortOrder(templateId);
    }

    @Override
    public Optional<TrainingDay> findById(UUID trainingDayId) {
        return jpa.findById(trainingDayId);
    }

    @Override
    public void deleteAll(List<TrainingDay> trainingDays) {
        jpa.deleteAll(trainingDays);
    }

    @Override
    public List<TrainingDay> saveAll(List<TrainingDay> trainingDays) {
        return jpa.saveAll(trainingDays);
    }
}
