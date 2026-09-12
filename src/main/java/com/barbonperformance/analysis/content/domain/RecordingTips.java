package com.barbonperformance.analysis.content.domain;

import com.barbonperformance.analysis.organization.domain.Organization;
import com.barbonperformance.analysis.user.domain.AppUser;
import com.barbonperformance.analysis.video.domain.VideoAsset;
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
@Table(name = "recording_tips")
public class RecordingTips {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "org_id", nullable = false, unique = true)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_asset_id")
    private VideoAsset videoAsset;

    @Column(name = "pdf_object_key")
    private String pdfObjectKey;

    @Column(name = "pdf_content_type")
    private String pdfContentType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by_user_id")
    private AppUser updatedByUser;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private Instant updatedAt;

    protected RecordingTips() {
        // for JPA
    }

    public RecordingTips(Organization organization) {
        this.organization = organization;
    }

    public void attachVideo(VideoAsset videoAsset, AppUser updatedBy) {
        this.videoAsset = videoAsset;
        this.updatedByUser = updatedBy;
    }

    public void attachPdf(String objectKey, String contentType, AppUser updatedBy) {
        this.pdfObjectKey = objectKey;
        this.pdfContentType = contentType;
        this.updatedByUser = updatedBy;
    }

    public UUID getId() {
        return id;
    }

    public Organization getOrganization() {
        return organization;
    }

    public VideoAsset getVideoAsset() {
        return videoAsset;
    }

    public String getPdfObjectKey() {
        return pdfObjectKey;
    }

    public String getPdfContentType() {
        return pdfContentType;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
