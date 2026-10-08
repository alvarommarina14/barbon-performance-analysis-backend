package com.barbonperformance.analysis.strength.domain;

import com.barbonperformance.analysis.organization.domain.Organization;
import com.barbonperformance.analysis.user.domain.PlayerProfile;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "cycle_phase")
public class CyclePhase {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "org_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id")
    private PlayerProfile player;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    private ProgramTemplate template;

    @Column(name = "week_number", nullable = false)
    private int weekNumber;

    @Column(name = "phase_name", nullable = false)
    private String phaseName;

    @Column(nullable = false)
    private String goal;

    @Column
    private String effort;

    @Column(name = "rpe_range")
    private String rpeRange;

    @Column(name = "reps_left")
    private String repsLeft;

    @Column
    private String example;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private Instant updatedAt;

    protected CyclePhase() {
        // for JPA
    }

    private CyclePhase(Organization organization, PlayerProfile player, ProgramTemplate template, int weekNumber,
            WeekDetails details) {
        this.organization = organization;
        this.player = player;
        this.template = template;
        this.weekNumber = weekNumber;
        updateDetails(details);
    }

    public static CyclePhase forPlayer(Organization organization, PlayerProfile player, int weekNumber,
            WeekDetails details) {
        return new CyclePhase(organization, player, null, weekNumber, details);
    }

    public static CyclePhase forTemplate(ProgramTemplate template, int weekNumber, WeekDetails details) {
        return new CyclePhase(template.getOrganization(), null, template, weekNumber, details);
    }

    public void updateDetails(WeekDetails details) {
        this.phaseName = details.phaseName();
        this.goal = details.goal();
        this.effort = details.effort();
        this.rpeRange = details.rpeRange();
        this.repsLeft = details.repsLeft();
        this.example = details.example();
    }

    public WeekDetails details() {
        return new WeekDetails(phaseName, goal, effort, rpeRange, repsLeft, example);
    }

    public UUID getId() {
        return id;
    }

    public Organization getOrganization() {
        return organization;
    }

    public PlayerProfile getPlayer() {
        return player;
    }

    public ProgramTemplate getTemplate() {
        return template;
    }

    public int getWeekNumber() {
        return weekNumber;
    }

    public String getPhaseName() {
        return phaseName;
    }

    public String getGoal() {
        return goal;
    }

    public String getEffort() {
        return effort;
    }

    public String getRpeRange() {
        return rpeRange;
    }

    public String getRepsLeft() {
        return repsLeft;
    }

    public String getExample() {
        return example;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
