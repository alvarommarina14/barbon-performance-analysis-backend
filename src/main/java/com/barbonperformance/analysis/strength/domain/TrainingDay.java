package com.barbonperformance.analysis.strength.domain;

import com.barbonperformance.analysis.organization.domain.Organization;
import com.barbonperformance.analysis.user.domain.PlayerProfile;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "training_day")
public class TrainingDay {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "org_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    private PlayerProfile player;

    @Column(name = "day_label", nullable = false)
    private String dayLabel;

    @Column(nullable = false)
    private String title;

    @Column
    private String focus;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TrainingDayKind kind;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private Instant updatedAt;

    protected TrainingDay() {
        // for JPA
    }

    public TrainingDay(Organization organization, PlayerProfile player, String dayLabel, String title,
            String focus, Integer durationMinutes, TrainingDayKind kind, int sortOrder) {
        this.organization = organization;
        this.player = player;
        this.dayLabel = dayLabel;
        this.title = title;
        this.focus = focus;
        this.durationMinutes = durationMinutes;
        this.kind = kind;
        this.sortOrder = sortOrder;
    }

    public void revise(String dayLabel, String title, String focus, Integer durationMinutes,
            TrainingDayKind kind, int sortOrder) {
        this.dayLabel = dayLabel;
        this.title = title;
        this.focus = focus;
        this.durationMinutes = durationMinutes;
        this.kind = kind;
        this.sortOrder = sortOrder;
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

    public String getDayLabel() {
        return dayLabel;
    }

    public String getTitle() {
        return title;
    }

    public String getFocus() {
        return focus;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public TrainingDayKind getKind() {
        return kind;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
