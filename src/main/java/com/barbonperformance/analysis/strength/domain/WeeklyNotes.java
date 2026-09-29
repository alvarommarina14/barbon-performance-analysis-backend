package com.barbonperformance.analysis.strength.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WeeklyNotes {

    List<WeeklyNote> findAllByPlayerId(UUID playerProfileId);

    Optional<WeeklyNote> findByPlayerIdAndPlanWeek(UUID playerProfileId, int planWeek);

    WeeklyNote save(WeeklyNote note);

    void delete(WeeklyNote note);
}
