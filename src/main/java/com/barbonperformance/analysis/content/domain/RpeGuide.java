package com.barbonperformance.analysis.content.domain;

import com.barbonperformance.analysis.organization.domain.Organization;
import com.barbonperformance.analysis.user.domain.AppUser;
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
@Table(name = "rpe_guide")
public class RpeGuide {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "org_id", nullable = false, unique = true)
    private Organization organization;

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

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private Instant updatedAt;

    protected RpeGuide() {
        // for JPA
    }

    public RpeGuide(Organization organization) {
        this.organization = organization;
    }

    public void attachPdf(String objectKey, String contentType, String fileName, long sizeBytes,
            AppUser updatedBy) {
        this.pdfObjectKey = objectKey;
        this.pdfContentType = contentType;
        this.pdfFileName = fileName;
        this.pdfSizeBytes = sizeBytes;
        this.pdfUploadedAt = Instant.now();
        this.updatedByUser = updatedBy;
    }

    public UUID getId() {
        return id;
    }

    public Organization getOrganization() {
        return organization;
    }

    public String getPdfObjectKey() {
        return pdfObjectKey;
    }

    public String getPdfContentType() {
        return pdfContentType;
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
