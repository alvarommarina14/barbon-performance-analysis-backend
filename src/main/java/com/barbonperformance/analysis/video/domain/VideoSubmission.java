package com.barbonperformance.analysis.video.domain;

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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "video_submission")
public class VideoSubmission {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "org_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    private PlayerProfile player;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "video_asset_id", nullable = false, unique = true)
    private VideoAsset videoAsset;

    @Enumerated(EnumType.STRING)
    @Column(name = "stroke_type", nullable = false)
    private StrokeType strokeType;

    @Column(name = "player_note")
    private String playerNote;

    @Column(name = "recorded_at")
    private Instant recordedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubmissionStatus status = SubmissionStatus.SUBMITTED;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private Instant updatedAt;

    protected VideoSubmission() {
        // for JPA
    }

    public VideoSubmission(Organization organization, PlayerProfile player, VideoAsset videoAsset,
            StrokeType strokeType, String playerNote, Instant recordedAt) {
        this.organization = organization;
        this.player = player;
        this.videoAsset = videoAsset;
        this.strokeType = strokeType;
        this.playerNote = playerNote;
        this.recordedAt = recordedAt;
    }

    public void takeIntoReview() {
        this.status = SubmissionStatus.IN_REVIEW;
    }

    // The coach abandoned a draft analysis: the video is back in the queue for the next attempt.
    public void returnToQueue() {
        this.status = SubmissionStatus.SUBMITTED;
    }

    public void markAnalyzed() {
        this.status = SubmissionStatus.ANALYZED;
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

    public VideoAsset getVideoAsset() {
        return videoAsset;
    }

    public StrokeType getStrokeType() {
        return strokeType;
    }

    public String getPlayerNote() {
        return playerNote;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }

    public SubmissionStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
