package com.barbonperformance.analysis.analysis.application;

import com.barbonperformance.analysis.analysis.application.dto.AnalysisDtos.AnalysisDetail;
import com.barbonperformance.analysis.analysis.application.dto.AnalysisDtos.CreateAnalysisRequest;
import com.barbonperformance.analysis.analysis.application.dto.AnalysisDtos.FeedbackUploadIntentRequest;
import com.barbonperformance.analysis.analysis.application.dto.AnalysisDtos.FeedbackUploadIntentResponse;
import com.barbonperformance.analysis.analysis.application.dto.AnalysisDtos.ReplaceFindingsRequest;
import com.barbonperformance.analysis.analysis.application.dto.AnalysisDtos.UpdateAnalysisRequest;
import com.barbonperformance.analysis.analysis.domain.Analyses;
import com.barbonperformance.analysis.analysis.domain.Analysis;
import com.barbonperformance.analysis.analysis.domain.AnalysisNotEditableException;
import com.barbonperformance.analysis.shared.domain.ConflictException;
import com.barbonperformance.analysis.shared.domain.NotFoundException;
import com.barbonperformance.analysis.user.domain.CoachProfile;
import com.barbonperformance.analysis.user.domain.CoachProfiles;
import com.barbonperformance.analysis.user.domain.PlayerProfile;
import com.barbonperformance.analysis.user.domain.PlayerProfiles;
import com.barbonperformance.analysis.video.VideoProperties;
import com.barbonperformance.analysis.video.domain.StreamGateway;
import com.barbonperformance.analysis.video.domain.SubmissionStatus;
import com.barbonperformance.analysis.video.domain.VideoAsset;
import com.barbonperformance.analysis.video.domain.VideoAssetKind;
import com.barbonperformance.analysis.video.domain.VideoAssets;
import com.barbonperformance.analysis.video.domain.VideoSubmission;
import com.barbonperformance.analysis.video.domain.VideoSubmissions;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalysisService {

    private final Analyses analyses;
    private final CoachProfiles coachProfiles;
    private final PlayerProfiles playerProfiles;
    private final VideoSubmissions submissions;
    private final VideoAssets videoAssets;
    private final StreamGateway stream;
    private final VideoProperties properties;

    public AnalysisService(Analyses analyses, CoachProfiles coachProfiles,
            PlayerProfiles playerProfiles, VideoSubmissions submissions, VideoAssets videoAssets,
            StreamGateway stream, VideoProperties properties) {
        this.analyses = analyses;
        this.coachProfiles = coachProfiles;
        this.playerProfiles = playerProfiles;
        this.submissions = submissions;
        this.videoAssets = videoAssets;
        this.stream = stream;
        this.properties = properties;
    }

    @Transactional
    public AnalysisDetail create(UUID coachUserId, CreateAnalysisRequest request) {
        CoachProfile coach = coach(coachUserId);

        PlayerProfile player;
        VideoSubmission submission;
        if (request.submissionId() != null) {
            submission = submissions.findById(request.submissionId())
                    .orElseThrow(() -> new NotFoundException(
                            "Submission " + request.submissionId() + " not found"));
            player = submission.getPlayer();
            requirePlayerBelongsToCoach(player, coach);
            if (analyses.findBySubmissionId(submission.getId()).isPresent()) {
                throw new ConflictException("that submission already has an analysis");
            }
            submission.takeIntoReview();
        } else {
            player = playerProfiles.findById(request.playerProfileId())
                    .orElseThrow(() -> new NotFoundException(
                            "Player " + request.playerProfileId() + " not found"));
            requirePlayerBelongsToCoach(player, coach);
            submission = null;
        }

        Analysis analysis = analyses.save(new Analysis(
                coach.getOrganization(), coach, player, submission, request.title(), request.summary()));
        return AnalysisMapper.detail(analysis);
    }

    @Transactional
    public AnalysisDetail update(UUID coachUserId, UUID analysisId, UpdateAnalysisRequest request) {
        Analysis analysis = loadOwned(coachUserId, analysisId);
        analysis.updateDraft(request.title(), request.summary());
        return AnalysisMapper.detail(analysis);
    }

    @Transactional
    public AnalysisDetail replaceFindings(UUID coachUserId, UUID analysisId,
            ReplaceFindingsRequest request) {
        Analysis analysis = loadOwned(coachUserId, analysisId);
        List<Analysis.FindingDraft> drafts = request.findings().stream()
                .map(finding -> new Analysis.FindingDraft(
                        finding.timestampSeconds(), finding.title(), finding.detail()))
                .toList();
        analysis.replaceFindings(drafts);
        return AnalysisMapper.detail(analysis);
    }

    @Transactional
    public FeedbackUploadIntentResponse requestFeedbackUploadIntent(UUID coachUserId, UUID analysisId,
            FeedbackUploadIntentRequest request) {
        Analysis analysis = loadOwned(coachUserId, analysisId);
        if (!analysis.isDraft()) {
            throw new AnalysisNotEditableException("this analysis is already published");
        }

        VideoAsset asset = videoAssets.save(
                new VideoAsset(analysis.getOrganization(), VideoAssetKind.COACH_FEEDBACK));

        StreamGateway.DirectUpload upload = stream.createDirectUpload(
                new StreamGateway.DirectUploadRequest(
                        analysis.getCoach().getId().toString(),
                        properties.maxDurationSeconds(),
                        request.sizeBytes(),
                        Map.of("analysisId", analysis.getId().toString())));

        asset.attachToStream(upload.streamUid());
        analysis.attachFeedbackVideo(asset);

        return new FeedbackUploadIntentResponse(
                analysis.getId(), asset.getId(), upload.uploadUrl(), properties.maxDurationSeconds());
    }

    @Transactional
    public AnalysisDetail publish(UUID coachUserId, UUID analysisId) {
        Analysis analysis = loadOwned(coachUserId, analysisId);
        analysis.publish(Instant.now());
        if (analysis.getSubmission() != null) {
            analysis.getSubmission().markAnalyzed();
        }
        return AnalysisMapper.detail(analysis);
    }

    @Transactional
    public void deleteDraft(UUID coachUserId, UUID analysisId) {
        Analysis analysis = loadOwned(coachUserId, analysisId);
        if (!analysis.isDraft()) {
            throw new AnalysisNotEditableException("a published analysis cannot be deleted");
        }
        VideoSubmission submission = analysis.getSubmission();
        if (submission != null && submission.getStatus() == SubmissionStatus.IN_REVIEW) {
            submission.returnToQueue();
        }
        analyses.delete(analysis);
    }

    private CoachProfile coach(UUID coachUserId) {
        return coachProfiles.findByUserId(coachUserId)
                .orElseThrow(() -> new NotFoundException("No coach profile for this account"));
    }

    // An analysis a coach does not own is reported as missing rather than forbidden, so the
    // endpoint cannot be used to tell an existing id from a free one.
    private Analysis loadOwned(UUID coachUserId, UUID analysisId) {
        CoachProfile coach = coach(coachUserId);
        Analysis analysis = analyses.findDetailedById(analysisId)
                .orElseThrow(() -> new NotFoundException("Analysis " + analysisId + " not found"));
        if (!analysis.getCoach().getId().equals(coach.getId())) {
            throw new NotFoundException("Analysis " + analysisId + " not found");
        }
        return analysis;
    }

    private void requirePlayerBelongsToCoach(PlayerProfile player, CoachProfile coach) {
        if (player.getCoach() == null || !player.getCoach().getId().equals(coach.getId())) {
            throw new AccessDeniedException("That player is not one of your clients");
        }
    }
}
