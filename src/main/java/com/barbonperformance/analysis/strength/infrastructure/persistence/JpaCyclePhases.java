package com.barbonperformance.analysis.strength.infrastructure.persistence;

import com.barbonperformance.analysis.strength.domain.CyclePhase;
import com.barbonperformance.analysis.strength.domain.CyclePhases;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
class JpaCyclePhases implements CyclePhases {

    private final CyclePhaseJpaRepository jpa;

    JpaCyclePhases(CyclePhaseJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public List<CyclePhase> findAllByPlayerId(UUID playerProfileId) {
        return jpa.findAllByPlayerIdOrderByWeekNumber(playerProfileId);
    }

    @Override
    public Optional<CyclePhase> findByPlayerIdAndWeekNumber(UUID playerProfileId, int weekNumber) {
        return jpa.findByPlayerIdAndWeekNumber(playerProfileId, weekNumber);
    }

    @Override
    public List<CyclePhase> findAllByTemplateId(UUID templateId) {
        return jpa.findAllByTemplateIdOrderByWeekNumber(templateId);
    }

    @Override
    public List<CyclePhase> saveAll(List<CyclePhase> phases) {
        return jpa.saveAll(phases);
    }
}
