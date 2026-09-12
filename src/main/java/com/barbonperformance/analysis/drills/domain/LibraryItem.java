package com.barbonperformance.analysis.drills.domain;

import com.barbonperformance.analysis.organization.domain.Organization;
import com.barbonperformance.analysis.video.domain.VideoAsset;
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
@Table(name = "video_library_item")
public class LibraryItem {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "org_id", nullable = false)
    private Organization organization;

    @Column(nullable = false)
    private String title;

    @Column
    private String cue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DrillCategory category;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "video_asset_id", nullable = false)
    private VideoAsset videoAsset;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private Instant updatedAt;

    protected LibraryItem() {
        // for JPA
    }

    public LibraryItem(Organization organization, String title, String cue, DrillCategory category,
            VideoAsset videoAsset) {
        this.organization = organization;
        this.title = title;
        this.cue = cue;
        this.category = category;
        this.videoAsset = videoAsset;
    }

    public void updateDetails(String title, String cue, DrillCategory category) {
        this.title = title;
        this.cue = cue;
        this.category = category;
    }

    public UUID getId() {
        return id;
    }

    public Organization getOrganization() {
        return organization;
    }

    public String getTitle() {
        return title;
    }

    public String getCue() {
        return cue;
    }

    public DrillCategory getCategory() {
        return category;
    }

    public VideoAsset getVideoAsset() {
        return videoAsset;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
