package com.barbonperformance.analysis.strength.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CyclePhases {

    List<CyclePhase> findAllByPlayerId(UUID playerProfileId);

    Optional<CyclePhase> findByPlayerIdAndWeekNumber(UUID playerProfileId, int weekNumber);

    List<CyclePhase> findAllTemplateRowsByOrgId(UUID orgId);

    List<CyclePhase> findAllByOrgIdAndTemplateName(UUID orgId, String templateName);

    boolean existsByOrgIdAndTemplateName(UUID orgId, String templateName);

    void deleteAllByOrgIdAndTemplateName(UUID orgId, String templateName);

    List<CyclePhase> saveAll(List<CyclePhase> phases);
}
