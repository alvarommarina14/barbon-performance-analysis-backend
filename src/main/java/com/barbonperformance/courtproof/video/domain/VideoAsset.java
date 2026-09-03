package com.barbonperformance.courtproof.video.domain;

import com.barbonperformance.courtproof.organization.domain.Organization;
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
@Table(name = "video_asset")
public class VideoAsset {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "org_id", nullable = false)
    private Organization organization;

    @Column(name = "stream_uid")
    private String streamUid;

    @Column(name = "playback_id")
    private String playbackId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VideoAssetKind kind;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VideoAssetStatus status = VideoAssetStatus.PENDING_UPLOAD;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private Instant updatedAt;

    protected VideoAsset() {
        // for JPA
    }

    public VideoAsset(Organization organization, VideoAssetKind kind) {
        this.organization = organization;
        this.kind = kind;
    }

    public void attachToStream(String streamUid) {
        if (this.streamUid != null && !this.streamUid.equals(streamUid)) {
            throw new IllegalStateException("Asset " + id + " is already bound to a Stream video");
        }
        this.streamUid = streamUid;
        this.status = VideoAssetStatus.UPLOADING;
    }

    // Every transition below answers false when it is a no-op, which is what makes replaying a
    // Cloudflare webhook harmless: the caller can tell a real change from a duplicate delivery.
    public boolean markProcessing() {
        if (status == VideoAssetStatus.PROCESSING || status == VideoAssetStatus.READY) {
            return false;
        }
        this.status = VideoAssetStatus.PROCESSING;
        return true;
    }

    public boolean markReady(String playbackId, Integer durationSeconds, String thumbnailUrl,
            Long sizeBytes) {
        if (status == VideoAssetStatus.READY) {
            return false;
        }
        this.playbackId = playbackId;
        this.durationSeconds = durationSeconds;
        this.thumbnailUrl = thumbnailUrl;
        this.sizeBytes = sizeBytes;
        this.status = VideoAssetStatus.READY;
        return true;
    }

    public boolean markErrored() {
        if (status == VideoAssetStatus.ERRORED) {
            return false;
        }
        this.status = VideoAssetStatus.ERRORED;
        return true;
    }

    public boolean isReady() {
        return status == VideoAssetStatus.READY;
    }

    public UUID getId() {
        return id;
    }

    public Organization getOrganization() {
        return organization;
    }

    public String getStreamUid() {
        return streamUid;
    }

    public String getPlaybackId() {
        return playbackId;
    }

    public VideoAssetKind getKind() {
        return kind;
    }

    public VideoAssetStatus getStatus() {
        return status;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public Long getSizeBytes() {
        return sizeBytes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
