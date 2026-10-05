package com.barbonperformance.analysis.content.domain;

import java.util.Optional;
import java.util.UUID;

public interface DailyRoutineStore {

    Optional<DailyRoutine> findByOrgId(UUID orgId);

    DailyRoutine save(DailyRoutine routine);
}
