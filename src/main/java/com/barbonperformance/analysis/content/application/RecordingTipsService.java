package com.barbonperformance.analysis.content.application;

import com.barbonperformance.analysis.content.ContentProperties;
import com.barbonperformance.analysis.content.application.dto.ContentDtos.ConfirmPdfRequest;
import com.barbonperformance.analysis.content.application.dto.ContentDtos.ConfirmVideoRequest;
import com.barbonperformance.analysis.content.application.dto.ContentDtos.PdfUploadIntentRequest;
import com.barbonperformance.analysis.content.application.dto.ContentDtos.PdfUploadIntentResponse;
import com.barbonperformance.analysis.content.application.dto.ContentDtos.RecordingTipsPlaybackView;
import com.barbonperformance.analysis.content.application.dto.ContentDtos.RecordingTipsView;
import com.barbonperformance.analysis.content.application.dto.ContentDtos.VideoUploadIntentRequest;
import com.barbonperformance.analysis.content.application.dto.ContentDtos.VideoUploadIntentResponse;
import com.barbonperformance.analysis.content.domain.ObjectStorage;
import com.barbonperformance.analysis.content.domain.RecordingTips;
import com.barbonperformance.analysis.content.domain.RecordingTipsStore;
import com.barbonperformance.analysis.organization.domain.Organization;
import com.barbonperformance.analysis.shared.domain.NotFoundException;
import com.barbonperformance.analysis.user.domain.AppUsers;
import com.barbonperformance.analysis.user.domain.CoachProfile;
import com.barbonperformance.analysis.user.domain.CoachProfiles;
import com.barbonperformance.analysis.video.VideoProperties;
import com.barbonperformance.analysis.video.domain.StreamGateway;
import com.barbonperformance.analysis.video.domain.VideoAsset;
import com.barbonperformance.analysis.video.domain.VideoAssetKind;
import com.barbonperformance.analysis.video.domain.VideoAssets;
import com.barbonperformance.analysis.video.domain.VideoNotReadyException;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecordingTipsService {

    private static final String PDF_KEY_TEMPLATE = "content/%s/recording-tips/guide.pdf";

    private final AppUsers users;
    private final CoachProfiles coachProfiles;
    private final RecordingTipsStore store;
    private final VideoAssets videoAssets;
    private final StreamGateway stream;
    private final VideoProperties videoProperties;
    private final ObjectStorage objectStorage;
    private final ContentProperties contentProperties;

    public RecordingTipsService(AppUsers users, CoachProfiles coachProfiles, RecordingTipsStore store,
            VideoAssets videoAssets, StreamGateway stream, VideoProperties videoProperties,
            ObjectStorage objectStorage, ContentProperties contentProperties) {
        this.users = users;
        this.coachProfiles = coachProfiles;
        this.store = store;
        this.videoAssets = videoAssets;
        this.stream = stream;
        this.videoProperties = videoProperties;
        this.objectStorage = objectStorage;
        this.contentProperties = contentProperties;
    }

    // Nothing uploaded yet is a valid state (a brand new org), not a 404.
    @Transactional(readOnly = true)
    public RecordingTipsView get(UUID userId) {
        return store.findByOrgId(organizationId(userId)).map(this::describe)
                .orElseGet(() -> new RecordingTipsView(null, null, null, null));
    }

    // Its own entitlement, same as drills: org-wide instructional content has no submission to
    // check ownership against, so GET /api/v1/videos/{id}/playback (submission-based) can never
    // authorize it.
    @Transactional(readOnly = true)
    public RecordingTipsPlaybackView playback(UUID userId) {
        RecordingTips tips = store.findByOrgId(organizationId(userId))
                .filter(t -> t.getVideoAsset() != null)
                .orElseThrow(() -> new NotFoundException("No recording-tips video has been uploaded yet"));

        VideoAsset asset = tips.getVideoAsset();
        if (!asset.isReady()) {
            throw new VideoNotReadyException("the video is still uploading");
        }

        StreamGateway.Playback playback =
                stream.signPlayback(asset.getStreamUid(), videoProperties.playbackTokenTtl());
        return new RecordingTipsPlaybackView(
                asset.getId(), playback.hlsUrl(), playback.dashUrl(), playback.token(),
                playback.expiresAt());
    }

    private UUID organizationId(UUID userId) {
        return users.findById(userId)
                .orElseThrow(() -> new NotFoundException("User " + userId + " not found"))
                .getOrganization().getId();
    }

    @Transactional
    public VideoUploadIntentResponse requestVideoUploadIntent(UUID coachUserId,
            VideoUploadIntentRequest request) {
        CoachProfile coach = coach(coachUserId);

        VideoAsset asset = videoAssets.save(
                new VideoAsset(coach.getOrganization(), VideoAssetKind.RECORDING_TIPS));

        StreamGateway.DirectUpload upload = stream.createDirectUpload(
                new StreamGateway.DirectUploadRequest(
                        coach.getId().toString(),
                        videoProperties.maxDurationSeconds(),
                        request.sizeBytes(),
                        Map.of("videoAssetId", asset.getId().toString())));

        asset.attachToStream(upload.streamUid());

        return new VideoUploadIntentResponse(
                asset.getId(), upload.uploadUrl(), videoProperties.maxDurationSeconds());
    }

    @Transactional
    public RecordingTipsView confirmVideo(UUID coachUserId, ConfirmVideoRequest request) {
        CoachProfile coach = coach(coachUserId);

        VideoAsset asset = videoAssets.findById(request.videoAssetId())
                .filter(a -> a.getKind() == VideoAssetKind.RECORDING_TIPS)
                .filter(a -> a.getOrganization().getId().equals(coach.getOrganization().getId()))
                .orElseThrow(() -> new NotFoundException(
                        "Video " + request.videoAssetId() + " not found"));
        if (!asset.isReady()) {
            throw new VideoNotReadyException("the video is still uploading");
        }

        RecordingTips tips = tipsFor(coach.getOrganization());
        tips.attachVideo(asset, coach.getUser());
        return describe(store.save(tips));
    }

    @Transactional
    public PdfUploadIntentResponse requestPdfUploadIntent(UUID coachUserId,
            PdfUploadIntentRequest request) {
        CoachProfile coach = coach(coachUserId);
        String key = PDF_KEY_TEMPLATE.formatted(coach.getOrganization().getId());
        ObjectStorage.PresignedUpload upload =
                objectStorage.presignUpload(key, contentProperties.presignTtl(), request.contentType());
        return new PdfUploadIntentResponse(upload.uploadUrl(), upload.objectKey());
    }

    @Transactional
    public RecordingTipsView confirmPdf(UUID coachUserId, ConfirmPdfRequest request) {
        CoachProfile coach = coach(coachUserId);
        RecordingTips tips = tipsFor(coach.getOrganization());
        tips.attachPdf(request.objectKey(), request.contentType(), coach.getUser());
        return describe(store.save(tips));
    }

    private RecordingTips tipsFor(Organization organization) {
        return store.findByOrgId(organization.getId())
                .orElseGet(() -> new RecordingTips(organization));
    }

    private RecordingTipsView describe(RecordingTips tips) {
        VideoAsset asset = tips.getVideoAsset();
        String pdfUrl = tips.getPdfObjectKey() == null ? null
                : objectStorage.presignDownload(tips.getPdfObjectKey(), contentProperties.presignTtl());
        return new RecordingTipsView(
                asset == null ? null : asset.getId(),
                asset == null ? null : asset.getStatus(),
                pdfUrl,
                tips.getUpdatedAt());
    }

    private CoachProfile coach(UUID coachUserId) {
        return coachProfiles.findByUserId(coachUserId)
                .orElseThrow(() -> new NotFoundException("No coach profile for this account"));
    }
}
