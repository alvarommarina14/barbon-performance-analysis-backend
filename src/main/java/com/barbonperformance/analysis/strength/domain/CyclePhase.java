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

    @Column(name = "template_name")
    private String templateName;

    @Column(name = "week_number", nullable = false)
    private int weekNumber;

    @Column(name = "phase_name", nullable = false)
    private String phaseName;

    @Column(nullable = false)
    private String goal;

    @Column(name = "sets_reps")
    private String setsReps;

    @Column(name = "rpe_range")
    private String rpeRange;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private Instant updatedAt;

    protected CyclePhase() {
        // for JPA
    }

    private CyclePhase(Organization organization, PlayerProfile player, String templateName, int weekNumber,
            String phaseName, String goal, String setsReps, String rpeRange) {
        this.organization = organization;
        this.player = player;
        this.templateName = templateName;
        this.weekNumber = weekNumber;
        this.phaseName = phaseName;
        this.goal = goal;
        this.setsReps = setsReps;
        this.rpeRange = rpeRange;
    }

    public static CyclePhase forPlayer(Organization organization, PlayerProfile player, int weekNumber,
            String phaseName, String goal, String setsReps, String rpeRange) {
        return new CyclePhase(organization, player, null, weekNumber, phaseName, goal, setsReps, rpeRange);
    }

    public static CyclePhase forTemplate(Organization organization, String templateName, int weekNumber,
            String phaseName, String goal, String setsReps, String rpeRange) {
        return new CyclePhase(organization, null, templateName, weekNumber, phaseName, goal, setsReps, rpeRange);
    }

    public void updateDetails(String phaseName, String goal, String setsReps, String rpeRange) {
        this.phaseName = phaseName;
        this.goal = goal;
        this.setsReps = setsReps;
        this.rpeRange = rpeRange;
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

    public String getTemplateName() {
        return templateName;
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

    public String getSetsReps() {
        return setsReps;
    }

    public String getRpeRange() {
        return rpeRange;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
