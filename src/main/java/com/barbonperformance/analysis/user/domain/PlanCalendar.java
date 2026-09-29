package com.barbonperformance.analysis.user.domain;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

public final class PlanCalendar {

    public static final int PLAN_WEEKS = 12;
    public static final int CYCLE_WEEKS = 4;

    private PlanCalendar() {
    }

    // Weeks turn over at midnight where the player lives, not where the server runs.
    public static LocalDate today(PlayerProfile player) {
        return LocalDate.now(ZoneId.of(player.getUser().getTimezone()));
    }

    public static Optional<Integer> planWeek(PlayerProfile player) {
        return planWeek(player.getPlanStartedAt(), today(player));
    }

    public static int unlockedWeeks(PlayerProfile player) {
        return unlockedWeeks(player.getPlanStartedAt(), today(player));
    }

    public static Optional<Integer> planWeek(LocalDate startDate, LocalDate today) {
        int week = weeksStarted(startDate, today);
        return week >= 1 && week <= PLAN_WEEKS ? Optional.of(week) : Optional.empty();
    }

    // Weeks the player has reached, including a finished plan: 0 before the start, 12 after the end.
    public static int unlockedWeeks(LocalDate startDate, LocalDate today) {
        return Math.min(Math.max(weeksStarted(startDate, today), 0), PLAN_WEEKS);
    }

    public static int cycleWeek(int planWeek) {
        requirePlanWeek(planWeek);
        return (planWeek - 1) % CYCLE_WEEKS + 1;
    }

    public static LocalDate weekStart(LocalDate startDate, int planWeek) {
        requirePlanWeek(planWeek);
        return startDate.plusWeeks(planWeek - 1L);
    }

    public static void requirePlanWeek(int planWeek) {
        if (planWeek < 1 || planWeek > PLAN_WEEKS) {
            throw new IllegalArgumentException("plan week must be between 1 and " + PLAN_WEEKS);
        }
    }

    private static int weeksStarted(LocalDate startDate, LocalDate today) {
        if (startDate == null || today.isBefore(startDate)) {
            return 0;
        }
        return (int) (ChronoUnit.DAYS.between(startDate, today) / 7) + 1;
    }
}
