package com.barbonperformance.analysis.analysis.domain;

import com.barbonperformance.analysis.organization.domain.Organization;
import com.barbonperformance.analysis.user.domain.CoachProfile;
import com.barbonperformance.analysis.user.domain.PlayerProfile;
import com.barbonperformance.analysis.video.domain.VideoAsset;
import com.barbonperformance.analysis.video.domain.VideoSubmission;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "analysis")
public class Analysis {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "org_id", nullable = false)
    private Organization organization;

    // Null when the coach filmed the player themselves rather than working from a submission.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_id")
    private VideoSubmission submission;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    private PlayerProfile player;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "coach_id", nullable = false)
    private CoachProfile coach;

    @Column(nullable = false)
    private String title;

    @Column
    private String summary;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feedback_video_asset_id")
    private VideoAsset feedbackVideoAsset;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AnalysisStatus status = AnalysisStatus.DRAFT;

    @Column(name = "published_at")
    private Instant publishedAt;

    @OneToMany(mappedBy = "analysis", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder")
    private final List<AnalysisFinding> findings = new ArrayList<>();

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private Instant updatedAt;

    protected Analysis() {
        // for JPA
    }

    public Analysis(Organization organization, CoachProfile coach, PlayerProfile player,
            VideoSubmission submission, String title, String summary) {
        this.organization = organization;
        this.coach = coach;
        this.player = player;
        this.submission = submission;
        this.title = title;
        this.summary = summary;
    }

    public void updateDraft(String title, String summary) {
        requireDraft();
        this.title = title;
        this.summary = summary;
    }

    public void replaceFindings(List<FindingDraft> drafts) {
        requireDraft();
        findings.clear();
        int order = 0;
        for (FindingDraft draft : drafts) {
            findings.add(new AnalysisFinding(
                    organization, this, draft.timestampSeconds(), draft.title(), draft.detail(), order++));
        }
    }

    public void attachFeedbackVideo(VideoAsset asset) {
        requireDraft();
        this.feedbackVideoAsset = asset;
    }

    public void publish(Instant now) {
        requireDraft();
        if (feedbackVideoAsset == null || !feedbackVideoAsset.isReady()) {
            throw new FeedbackVideoNotReadyException("the feedback video is not ready yet");
        }
        this.status = AnalysisStatus.PUBLISHED;
        this.publishedAt = now;
    }

    private void requireDraft() {
        if (status != AnalysisStatus.DRAFT) {
            throw new AnalysisNotEditableException("this analysis is already published");
        }
    }

    public boolean isDraft() {
        return status == AnalysisStatus.DRAFT;
    }

    public boolean feedbackReady() {
        return feedbackVideoAsset != null && feedbackVideoAsset.isReady();
    }

    public UUID getId() {
        return id;
    }

    public Organization getOrganization() {
        return organization;
    }

    public VideoSubmission getSubmission() {
        return submission;
    }

    public PlayerProfile getPlayer() {
        return player;
    }

    public CoachProfile getCoach() {
        return coach;
    }

    public String getTitle() {
        return title;
    }

    public String getSummary() {
        return summary;
    }

    public VideoAsset getFeedbackVideoAsset() {
        return feedbackVideoAsset;
    }

    public AnalysisStatus getStatus() {
        return status;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public List<AnalysisFinding> getFindings() {
        return findings;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public record FindingDraft(int timestampSeconds, String title, String detail) {
    }
}
