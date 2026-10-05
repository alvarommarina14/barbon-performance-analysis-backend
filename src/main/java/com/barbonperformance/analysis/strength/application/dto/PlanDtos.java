package com.barbonperformance.analysis.strength.application.dto;

import java.time.LocalDate;

public final class PlanDtos {

    private PlanDtos() {
    }

    public record SetPlanStartRequest(LocalDate startDate) {
    }

    public record PlanView(
            LocalDate startDate,
            Integer planWeek,
            int planWeeks,
            Integer cycleWeek,
            int unlockedWeeks) {
    }
}
