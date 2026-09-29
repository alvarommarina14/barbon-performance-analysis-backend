package com.barbonperformance.analysis.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.barbonperformance.analysis.user.domain.PlanCalendar;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class PlanCalendarTest {

    private static final LocalDate START = LocalDate.of(2026, 9, 7);

    @Test
    void theFirstDayOpensWeekOne() {
        assertThat(PlanCalendar.planWeek(START, START)).contains(1);
        assertThat(PlanCalendar.planWeek(START, START.plusDays(6))).contains(1);
    }

    @Test
    void weeksTurnOverEverySevenDays() {
        assertThat(PlanCalendar.planWeek(START, START.plusDays(7))).contains(2);
        assertThat(PlanCalendar.planWeek(START, START.plusWeeks(4))).contains(5);
        assertThat(PlanCalendar.planWeek(START, START.plusWeeks(11).plusDays(6))).contains(12);
    }

    @Test
    void thereIsNoPlanWeekBeforeTheStartOrAfterTheTwelfthWeek() {
        assertThat(PlanCalendar.planWeek(START, START.minusDays(1))).isEmpty();
        assertThat(PlanCalendar.planWeek(START, START.plusWeeks(12))).isEmpty();
        assertThat(PlanCalendar.planWeek(null, START)).isEmpty();
    }

    @Test
    void unlockedWeeksStopAtTheEdgesOfThePlan() {
        assertThat(PlanCalendar.unlockedWeeks(START, START.minusDays(1))).isZero();
        assertThat(PlanCalendar.unlockedWeeks(START, START.plusWeeks(2))).isEqualTo(3);
        assertThat(PlanCalendar.unlockedWeeks(START, START.plusWeeks(20))).isEqualTo(12);
    }

    @Test
    void theFourWeekCycleRepeatsThreeTimes() {
        assertThat(PlanCalendar.cycleWeek(1)).isEqualTo(1);
        assertThat(PlanCalendar.cycleWeek(4)).isEqualTo(4);
        assertThat(PlanCalendar.cycleWeek(5)).isEqualTo(1);
        assertThat(PlanCalendar.cycleWeek(12)).isEqualTo(4);
    }

    @Test
    void weekStartIsCountedFromThePlanStart() {
        assertThat(PlanCalendar.weekStart(START, 1)).isEqualTo(START);
        assertThat(PlanCalendar.weekStart(START, 5)).isEqualTo(START.plusWeeks(4));
    }

    @Test
    void weeksOutsideThePlanAreRejected() {
        assertThatThrownBy(() -> PlanCalendar.cycleWeek(13)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PlanCalendar.weekStart(START, 0)).isInstanceOf(IllegalArgumentException.class);
    }
}
