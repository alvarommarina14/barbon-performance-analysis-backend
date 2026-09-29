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
@Table(name = "daily_routine")
public class DailyRoutine {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "org_id", nullable = false, unique = true)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warmup_video_asset_id")
    private VideoAsset warmupVideo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recovery_video_asset_id")
    private VideoAsset recoveryVideo;

    @Column(name = "pdf_object_key")
    private String pdfObjectKey;

    @Column(name = "pdf_content_type")
    private String pdfContentType;

    @Column(name = "pdf_file_name")
    private String pdfFileName;

    @Column(name = "pdf_size_bytes")
    private Long pdfSizeBytes;

    @Column(name = "pdf_uploaded_at")
    private Instant pdfUploadedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by_user_id")
    private AppUser updatedByUser;

    // Set here as well as by the trigger, so the response to the first save already carries it.
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected DailyRoutine() {
        // for JPA
    }

    public DailyRoutine(Organization organization) {
        this.organization = organization;
        this.updatedAt = Instant.now();
    }

    public void attachVideo(DailyRoutinePart part, VideoAsset video, AppUser updatedBy) {
        if (part == DailyRoutinePart.WARMUP) {
            this.warmupVideo = video;
        } else {
            this.recoveryVideo = video;
        }
        touch(updatedBy);
    }

    public void attachPdf(String objectKey, String contentType, String fileName, long sizeBytes,
            AppUser updatedBy) {
        this.pdfObjectKey = objectKey;
        this.pdfContentType = contentType;
        this.pdfFileName = fileName;
        this.pdfSizeBytes = sizeBytes;
        this.pdfUploadedAt = Instant.now();
        touch(updatedBy);
    }

    private void touch(AppUser updatedBy) {
        this.updatedByUser = updatedBy;
        this.updatedAt = Instant.now();
    }

    public VideoAsset video(DailyRoutinePart part) {
        return part == DailyRoutinePart.WARMUP ? warmupVideo : recoveryVideo;
    }

    public UUID getId() {
        return id;
    }

    public String getPdfObjectKey() {
        return pdfObjectKey;
    }

    public String getPdfFileName() {
        return pdfFileName;
    }

    public Long getPdfSizeBytes() {
        return pdfSizeBytes;
    }

    public Instant getPdfUploadedAt() {
        return pdfUploadedAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
