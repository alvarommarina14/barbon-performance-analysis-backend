package com.barbonperformance.analysis.analysis.domain;

import com.barbonperformance.analysis.organization.domain.Organization;
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
@Table(name = "analysis_finding")
public class AnalysisFinding {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "org_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "analysis_id", nullable = false)
    private Analysis analysis;

    @Column(name = "timestamp_seconds", nullable = false)
    private int timestampSeconds;

    @Column(nullable = false)
    private String title;

    @Column
    private String detail;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private Instant updatedAt;

    protected AnalysisFinding() {
        // for JPA
    }

    AnalysisFinding(Organization organization, Analysis analysis, int timestampSeconds,
            String title, String detail, int sortOrder) {
        this.organization = organization;
        this.analysis = analysis;
        this.timestampSeconds = timestampSeconds;
        this.title = title;
        this.detail = detail;
        this.sortOrder = sortOrder;
    }

    public UUID getId() {
        return id;
    }

    public int getTimestampSeconds() {
        return timestampSeconds;
    }

    public String getTitle() {
        return title;
    }

    public String getDetail() {
        return detail;
    }

    public int getSortOrder() {
        return sortOrder;
    }
}
