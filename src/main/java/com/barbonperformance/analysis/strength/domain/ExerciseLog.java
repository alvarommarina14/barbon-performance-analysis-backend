package com.barbonperformance.analysis.strength.domain;

import com.barbonperformance.analysis.organization.domain.Organization;
import com.barbonperformance.analysis.user.domain.PlanCalendar;
import com.barbonperformance.analysis.user.domain.PlayerProfile;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "exercise_log")
public class ExerciseLog {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "org_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    private PlayerProfile player;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "day_exercise_id", nullable = false)
    private DayExercise dayExercise;

    @Column(name = "plan_week", nullable = false)
    private int planWeek;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WeightUnit unit;

    @Column(name = "logged_at", nullable = false)
    private Instant loggedAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "exercise_log_set", joinColumns = @JoinColumn(name = "log_id"))
    @OrderBy("setNumber")
    private List<LoggedSet> sets = new ArrayList<>();

    protected ExerciseLog() {
        // for JPA
    }

    public ExerciseLog(PlayerProfile player, DayExercise dayExercise, int planWeek) {
        PlanCalendar.requirePlanWeek(planWeek);
        this.organization = player.getOrganization();
        this.player = player;
        this.dayExercise = dayExercise;
        this.planWeek = planWeek;
    }

    public void record(WeightUnit unit, List<LoggedSet> sets) {
        this.unit = unit;
        this.sets.clear();
        this.sets.addAll(sets);
        this.loggedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public DayExercise getDayExercise() {
        return dayExercise;
    }

    public int getPlanWeek() {
        return planWeek;
    }

    public WeightUnit getUnit() {
        return unit;
    }

    public Instant getLoggedAt() {
        return loggedAt;
    }

    public List<LoggedSet> getSets() {
        return List.copyOf(sets);
    }
}
