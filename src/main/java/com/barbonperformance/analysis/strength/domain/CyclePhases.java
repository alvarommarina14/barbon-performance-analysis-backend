package com.barbonperformance.analysis.strength.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CyclePhases {

    List<CyclePhase> findAllByPlayerId(UUID playerProfileId);

    Optional<CyclePhase> findByPlayerIdAndWeekNumber(UUID playerProfileId, int weekNumber);

    List<CyclePhase> findAllByTemplateId(UUID templateId);

    List<CyclePhase> saveAll(List<CyclePhase> phases);
}
