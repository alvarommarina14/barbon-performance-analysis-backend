package com.barbonperformance.analysis.video.application;

import com.barbonperformance.analysis.shared.domain.NotFoundException;
import com.barbonperformance.analysis.user.domain.CoachProfiles;
import com.barbonperformance.analysis.user.domain.PlayerProfile;
import com.barbonperformance.analysis.user.domain.PlayerProfiles;
import com.barbonperformance.analysis.video.VideoProperties;
import com.barbonperformance.analysis.video.application.dto.VideoDtos.SubmissionSummary;
import com.barbonperformance.analysis.video.application.dto.VideoDtos.UploadIntentRequest;
import com.barbonperformance.analysis.video.application.dto.VideoDtos.UploadIntentResponse;
import com.barbonperformance.analysis.video.domain.StreamGateway;
import com.barbonperformance.analysis.video.domain.VideoAsset;
import com.barbonperformance.analysis.video.domain.VideoAssetKind;
import com.barbonperformance.analysis.video.domain.VideoAssets;
import com.barbonperformance.analysis.video.domain.VideoSubmission;
import com.barbonperformance.analysis.video.domain.VideoSubmissions;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VideoUploadService {

    private final PlayerProfiles playerProfiles;
    private final CoachProfiles coachProfiles;
    private final VideoAssets videoAssets;
    private final VideoSubmissions submissions;
    private final StreamGateway stream;
    private final VideoProperties properties;

    public VideoUploadService(PlayerProfiles playerProfiles, CoachProfiles coachProfiles,
            VideoAssets videoAssets, VideoSubmissions submissions, StreamGateway stream,
            VideoProperties properties) {
        this.playerProfiles = playerProfiles;
        this.coachProfiles = coachProfiles;
        this.videoAssets = videoAssets;
        this.submissions = submissions;
        this.stream = stream;
        this.properties = properties;
    }

    // The rows are written before Cloudflare is called, because the submission id goes into the
    // video's metadata: that is the only thing tying a webhook delivery back to a player.
    @Transactional
    public UploadIntentResponse requestUploadIntent(UUID userId, UploadIntentRequest request) {
        PlayerProfile player = playerProfiles.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("No player profile for this account"));

        VideoAsset asset = videoAssets.save(
                new VideoAsset(player.getOrganization(), VideoAssetKind.PLAYER_UPLOAD));

        VideoSubmission submission = submissions.save(new VideoSubmission(
                player.getOrganization(),
                player,
                asset,
                request.strokeType(),
                request.playerNote(),
                request.recordedAt()));

        StreamGateway.DirectUpload upload = stream.createDirectUpload(
                new StreamGateway.DirectUploadRequest(
                        player.getId().toString(),
                        properties.maxDurationSeconds(),
                        request.sizeBytes(),
                        Map.of("submissionId", submission.getId().toString())));

        asset.attachToStream(upload.streamUid());

        return new UploadIntentResponse(
                submission.getId(),
                asset.getId(),
                upload.uploadUrl(),
                properties.maxDurationSeconds());
    }

    @Transactional(readOnly = true)
    public List<SubmissionSummary> mySubmissions(UUID userId) {
        PlayerProfile player = playerProfiles.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("No player profile for this account"));
        return submissions.findAllByPlayerId(player.getId()).stream()
                .map(VideoUploadService::describe)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SubmissionSummary> coachQueue(UUID coachUserId) {
        UUID coachProfileId = coachProfiles.findByUserId(coachUserId)
                .orElseThrow(() -> new NotFoundException("No coach profile for this account"))
                .getId();
        return submissions.findQueueForCoach(coachProfileId).stream()
                .map(VideoUploadService::describe)
                .toList();
    }

    static SubmissionSummary describe(VideoSubmission submission) {
        VideoAsset asset = submission.getVideoAsset();
        return new SubmissionSummary(
                submission.getId(),
                submission.getPlayer().getId(),
                submission.getPlayer().getUser().getName(),
                submission.getStrokeType(),
                submission.getPlayerNote(),
                submission.getRecordedAt(),
                submission.getStatus(),
                asset.getStatus(),
                asset.getId(),
                asset.getDurationSeconds(),
                asset.getThumbnailUrl(),
                submission.getCreatedAt());
    }
}
