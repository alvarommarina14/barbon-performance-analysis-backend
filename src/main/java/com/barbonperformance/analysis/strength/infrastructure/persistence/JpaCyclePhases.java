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
    public List<CyclePhase> findAllTemplateRowsByOrgId(UUID orgId) {
        return jpa.findAllByOrganizationIdAndTemplateNameIsNotNullOrderByTemplateNameAscWeekNumberAsc(orgId);
    }

    @Override
    public List<CyclePhase> findAllByOrgIdAndTemplateName(UUID orgId, String templateName) {
        return jpa.findAllByOrganizationIdAndTemplateNameOrderByWeekNumber(orgId, templateName);
    }

    @Override
    public boolean existsByOrgIdAndTemplateName(UUID orgId, String templateName) {
        return jpa.existsByOrganizationIdAndTemplateName(orgId, templateName);
    }

    @Override
    public void deleteAllByOrgIdAndTemplateName(UUID orgId, String templateName) {
        jpa.deleteAllByOrganizationIdAndTemplateName(orgId, templateName);
    }

    @Override
    public List<CyclePhase> saveAll(List<CyclePhase> phases) {
        return jpa.saveAll(phases);
    }
}
