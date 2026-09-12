package com.barbonperformance.analysis.drills.domain;

import com.barbonperformance.analysis.analysis.domain.Analysis;
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
@Table(name = "drill_assignment")
public class DrillAssignment {

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
    @JoinColumn(name = "library_item_id", nullable = false)
    private LibraryItem libraryItem;

    // Nullable so a drill could one day be assigned outside of any analysis; every write path
    // today always sets it.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysis_id")
    private Analysis analysis;

    @Column(name = "coach_note")
    private String coachNote;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private Instant updatedAt;

    protected DrillAssignment() {
        // for JPA
    }

    public DrillAssignment(Organization organization, PlayerProfile player, LibraryItem libraryItem,
            Analysis analysis, String coachNote, int sortOrder) {
        this.organization = organization;
        this.player = player;
        this.libraryItem = libraryItem;
        this.analysis = analysis;
        this.coachNote = coachNote;
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

    public LibraryItem getLibraryItem() {
        return libraryItem;
    }

    public Analysis getAnalysis() {
        return analysis;
    }

    public String getCoachNote() {
        return coachNote;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
