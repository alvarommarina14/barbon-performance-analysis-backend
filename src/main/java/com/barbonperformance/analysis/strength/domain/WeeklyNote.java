package com.barbonperformance.analysis.strength.domain;

import com.barbonperformance.analysis.organization.domain.Organization;
import com.barbonperformance.analysis.user.domain.PlanCalendar;
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
@Table(name = "weekly_note")
public class WeeklyNote {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "org_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    private PlayerProfile player;

    @Column(name = "plan_week", nullable = false)
    private int planWeek;

    @Column(nullable = false)
    private String body;

    // Set here as well as by the table's trigger: the write endpoint answers with the note it just
    // saved, before any reload could pick up a database-generated value.
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected WeeklyNote() {
        // for JPA
    }

    public WeeklyNote(PlayerProfile player, int planWeek, String body) {
        PlanCalendar.requirePlanWeek(planWeek);
        this.organization = player.getOrganization();
        this.player = player;
        this.planWeek = planWeek;
        this.body = body;
        this.updatedAt = Instant.now();
    }

    public void rewrite(String body) {
        this.body = body;
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public int getPlanWeek() {
        return planWeek;
    }

    public String getBody() {
        return body;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
