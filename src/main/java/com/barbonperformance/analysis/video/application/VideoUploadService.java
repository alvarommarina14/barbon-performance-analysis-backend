package com.barbonperformance.analysis.video.application;

import com.barbonperformance.analysis.shared.domain.ConflictException;
import com.barbonperformance.analysis.shared.domain.NotFoundException;
import com.barbonperformance.analysis.user.domain.CoachProfiles;
import com.barbonperformance.analysis.user.domain.PlayerProfile;
import com.barbonperformance.analysis.user.domain.PlayerProfiles;
import com.barbonperformance.analysis.video.VideoProperties;
import com.barbonperformance.analysis.video.application.dto.VideoDtos.CreateSubmissionRequest;
import com.barbonperformance.analysis.video.application.dto.VideoDtos.SubmissionSummary;
import com.barbonperformance.analysis.video.application.dto.VideoDtos.UploadIntentRequest;
import com.barbonperformance.analysis.video.application.dto.VideoDtos.UploadIntentResponse;
import com.barbonperformance.analysis.video.domain.StreamGateway;
import com.barbonperformance.analysis.video.domain.VideoAsset;
import com.barbonperformance.analysis.video.domain.VideoAssetKind;
import com.barbonperformance.analysis.video.domain.VideoAssets;
import com.barbonperformance.analysis.video.domain.VideoNotReadyException;
import com.barbonperformance.analysis.video.domain.VideoSubmission;
import com.barbonperformance.analysis.video.domain.VideoSubmissions;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VideoUploadService {

    private static final Logger log = LoggerFactory.getLogger(VideoUploadService.class);

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

    // Only a video_asset is created here — enough to register the upload with the provider and
    // carry the stream uid the webhook matches on. The submission comes later, once the bytes
    // have landed, so a failed upload leaves nothing the player can see.
    @Transactional
    public UploadIntentResponse requestUploadIntent(UUID userId, UploadIntentRequest request) {
        PlayerProfile player = playerProfiles.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("No player profile for this account"));

        VideoAsset asset = videoAssets.save(new VideoAsset(
                player.getOrganization(), VideoAssetKind.PLAYER_UPLOAD, player));

        StreamGateway.DirectUpload upload = stream.createDirectUpload(
                new StreamGateway.DirectUploadRequest(
                        player.getId().toString(),
                        properties.maxDurationSeconds(),
                        request.sizeBytes(),
                        Map.of("videoAssetId", asset.getId().toString())));

        asset.attachToStream(upload.streamUid());

        return new UploadIntentResponse(
                asset.getId(), upload.uploadUrl(), properties.maxDurationSeconds());
    }

    // Called once the client's upload has succeeded. The asset must be this player's and READY
    // (the webhook has confirmed the video), so every submission has a playable video.
    @Transactional
    public SubmissionSummary createSubmission(UUID userId, CreateSubmissionRequest request) {
        PlayerProfile player = playerProfiles.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("No player profile for this account"));

        VideoAsset asset = videoAssets.findById(request.videoAssetId())
                .filter(a -> a.getKind() == VideoAssetKind.PLAYER_UPLOAD)
                .filter(a -> a.wasUploadedBy(player.getId()))
                .orElseThrow(() -> new NotFoundException(
                        "Video " + request.videoAssetId() + " not found"));

        if (submissions.findByAssetId(asset.getId()).isPresent()) {
            throw new ConflictException("that video already has a submission");
        }
        if (!asset.isReady()) {
            throw new VideoNotReadyException("the video is still uploading");
        }

        VideoSubmission submission = submissions.save(new VideoSubmission(
                player.getOrganization(), player, asset,
                request.playerNote(), request.recordedAt()));
        return describe(submission);
    }

    // Pull back a submission the coach has not started on — a "wrong clip, let me redo it".
    // The row and its asset go together (VideoSubmission cascades the remove).
    @Transactional
    public void deleteSubmission(UUID userId, UUID submissionId) {
        PlayerProfile player = playerProfiles.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("No player profile for this account"));
        VideoSubmission submission = submissions.findById(submissionId)
                .filter(s -> s.getPlayer().getId().equals(player.getId()))
                .orElseThrow(() -> new NotFoundException("Submission " + submissionId + " not found"));

        if (!submission.isCancellable()) {
            throw new ConflictException("your coach has already started on this video");
        }
        submissions.delete(submission);
    }

    // Housekeeping for uploads that never became a submission: the app was closed between the
    // upload and the create-submission call, or the upload itself never finished. The asset row
    // and its stored bytes are invisible to everyone, so they are just removed.
    @Transactional
    public int sweepOrphanedUploads() {
        Instant cutoff = Instant.now().minus(properties.orphanedUploadGrace());
        List<VideoAsset> orphans = videoAssets.findOrphanedUploads(cutoff);
        for (VideoAsset asset : orphans) {
            if (asset.getStreamUid() != null) {
                stream.discardUpload(asset.getStreamUid());
            }
            videoAssets.delete(asset);
        }
        if (!orphans.isEmpty()) {
            log.info("Swept {} upload(s) that never became a submission", orphans.size());
        }
        return orphans.size();
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
