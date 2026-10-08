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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id")
    private PlayerProfile player;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    private ProgramTemplate template;

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

    // A cardio day is described by this free text instead of a list of exercises.
    @Column
    private String protocol;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private Instant updatedAt;

    protected TrainingDay() {
        // for JPA
    }

    private TrainingDay(Organization organization, PlayerProfile player, ProgramTemplate template, String title,
            String focus, Integer durationMinutes, TrainingDayKind kind, String protocol, int sortOrder) {
        this.organization = organization;
        this.player = player;
        this.template = template;
        this.title = title;
        this.focus = focus;
        this.durationMinutes = durationMinutes;
        this.kind = kind;
        this.protocol = protocol;
        place(sortOrder);
    }

    public static TrainingDay forPlayer(PlayerProfile player, String title, String focus, Integer durationMinutes,
            TrainingDayKind kind, String protocol, int sortOrder) {
        return new TrainingDay(player.getOrganization(), player, null, title, focus, durationMinutes, kind, protocol,
                sortOrder);
    }

    public static TrainingDay forTemplate(ProgramTemplate template, String title, String focus,
            Integer durationMinutes, TrainingDayKind kind, String protocol, int sortOrder) {
        return new TrainingDay(template.getOrganization(), null, template, title, focus, durationMinutes, kind,
                protocol, sortOrder);
    }

    public void revise(String title, String focus, Integer durationMinutes, TrainingDayKind kind,
            String protocol, int sortOrder) {
        this.title = title;
        this.focus = focus;
        this.durationMinutes = durationMinutes;
        this.kind = kind;
        this.protocol = protocol;
        place(sortOrder);
    }

    private void place(int sortOrder) {
        this.sortOrder = sortOrder;
        this.dayLabel = "Day " + (sortOrder + 1);
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

    public String getProtocol() {
        return protocol;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
