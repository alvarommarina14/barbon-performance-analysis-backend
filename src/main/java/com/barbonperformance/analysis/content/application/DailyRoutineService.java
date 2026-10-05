package com.barbonperformance.analysis.content.application;

import com.barbonperformance.analysis.content.ContentProperties;
import com.barbonperformance.analysis.content.application.dto.ContentDtos.ConfirmPdfUploadRequest;
import com.barbonperformance.analysis.content.application.dto.ContentDtos.PdfUploadIntentRequest;
import com.barbonperformance.analysis.content.application.dto.ContentDtos.PdfUploadIntentResponse;
import com.barbonperformance.analysis.content.application.dto.ContentDtos.VideoUploadIntentResponse;
import com.barbonperformance.analysis.content.application.dto.DailyRoutineDtos.ConfirmRoutineVideoRequest;
import com.barbonperformance.analysis.content.application.dto.DailyRoutineDtos.DailyRoutinePlaybackView;
import com.barbonperformance.analysis.content.application.dto.DailyRoutineDtos.DailyRoutineView;
import com.barbonperformance.analysis.content.application.dto.DailyRoutineDtos.RoutineVideoUploadIntentRequest;
import com.barbonperformance.analysis.content.application.dto.DailyRoutineDtos.RoutineVideoView;
import com.barbonperformance.analysis.content.domain.DailyRoutine;
import com.barbonperformance.analysis.content.domain.DailyRoutinePart;
import com.barbonperformance.analysis.content.domain.DailyRoutineStore;
import com.barbonperformance.analysis.content.domain.ObjectStorage;
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
public class DailyRoutineService {

    private static final String PDF_KEY_TEMPLATE = "content/%s/daily-routine/routine.pdf";

    private final AppUsers users;
    private final CoachProfiles coachProfiles;
    private final DailyRoutineStore store;
    private final VideoAssets videoAssets;
    private final StreamGateway stream;
    private final VideoProperties videoProperties;
    private final ObjectStorage objectStorage;
    private final ContentProperties contentProperties;

    public DailyRoutineService(AppUsers users, CoachProfiles coachProfiles, DailyRoutineStore store,
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

    @Transactional(readOnly = true)
    public DailyRoutineView get(UUID userId) {
        return store.findByOrgId(organizationId(userId)).map(this::describe)
                .orElseGet(() -> new DailyRoutineView(null, null, null, null, null, null, null));
    }

    // Org-wide content has no submission to check ownership against, so it carries its own
    // entitlement rather than going through the submission-based playback endpoint.
    @Transactional(readOnly = true)
    public DailyRoutinePlaybackView playback(UUID userId, DailyRoutinePart part) {
        VideoAsset asset = store.findByOrgId(organizationId(userId))
                .map(routine -> routine.video(part))
                .orElseThrow(() -> new NotFoundException("No " + label(part) + " video has been uploaded yet"));
        if (!asset.isReady()) {
            throw new VideoNotReadyException("the video is still uploading");
        }
        StreamGateway.Playback playback =
                stream.signPlayback(asset.getStreamUid(), videoProperties.playbackTokenTtl());
        return new DailyRoutinePlaybackView(
                asset.getId(), playback.hlsUrl(), playback.dashUrl(), playback.token(), playback.expiresAt());
    }

    @Transactional
    public VideoUploadIntentResponse requestVideoUploadIntent(UUID coachUserId,
            RoutineVideoUploadIntentRequest request) {
        CoachProfile coach = coach(coachUserId);

        VideoAsset asset = new VideoAsset(coach.getOrganization(), VideoAssetKind.PROTOCOL);
        asset.nameFile(request.fileName());
        asset = videoAssets.save(asset);

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
    public DailyRoutineView confirmVideo(UUID coachUserId, ConfirmRoutineVideoRequest request) {
        CoachProfile coach = coach(coachUserId);

        VideoAsset asset = videoAssets.findById(request.videoAssetId())
                .filter(a -> a.getKind() == VideoAssetKind.PROTOCOL)
                .filter(a -> a.getOrganization().getId().equals(coach.getOrganization().getId()))
                .orElseThrow(() -> new NotFoundException("Video " + request.videoAssetId() + " not found"));
        if (!asset.isReady()) {
            throw new VideoNotReadyException("the video is still uploading");
        }

        DailyRoutine routine = routineFor(coach.getOrganization());
        routine.attachVideo(request.part(), asset, coach.getUser());
        return describe(store.save(routine));
    }

    @Transactional
    public PdfUploadIntentResponse requestPdfUploadIntent(UUID coachUserId, PdfUploadIntentRequest request) {
        CoachProfile coach = coach(coachUserId);
        ObjectStorage.PresignedUpload upload = objectStorage.presignUpload(
                pdfKey(coach.getOrganization()), contentProperties.presignTtl(), request.contentType());
        return new PdfUploadIntentResponse(upload.uploadUrl(), upload.objectKey());
    }

    @Transactional
    public DailyRoutineView confirmPdf(UUID coachUserId, ConfirmPdfUploadRequest request) {
        CoachProfile coach = coach(coachUserId);
        UploadedObjects.requireKey(request.objectKey(), pdfKey(coach.getOrganization()));

        DailyRoutine routine = routineFor(coach.getOrganization());
        routine.attachPdf(request.objectKey(), request.contentType(), request.fileName(), request.sizeBytes(),
                coach.getUser());
        return describe(store.save(routine));
    }

    private static String pdfKey(Organization organization) {
        return PDF_KEY_TEMPLATE.formatted(organization.getId());
    }

    private static String label(DailyRoutinePart part) {
        return part == DailyRoutinePart.WARMUP ? "warm-up" : "recovery";
    }

    private DailyRoutine routineFor(Organization organization) {
        return store.findByOrgId(organization.getId()).orElseGet(() -> new DailyRoutine(organization));
    }

    private DailyRoutineView describe(DailyRoutine routine) {
        String pdfUrl = routine.getPdfObjectKey() == null ? null
                : objectStorage.presignDownload(routine.getPdfObjectKey(), contentProperties.presignTtl());
        return new DailyRoutineView(
                describe(routine.video(DailyRoutinePart.WARMUP)),
                describe(routine.video(DailyRoutinePart.RECOVERY)),
                pdfUrl,
                routine.getPdfFileName(),
                routine.getPdfSizeBytes(),
                routine.getPdfUploadedAt(),
                routine.getUpdatedAt());
    }

    private static RoutineVideoView describe(VideoAsset asset) {
        if (asset == null) {
            return null;
        }
        return new RoutineVideoView(asset.getId(), asset.getStatus(), asset.getOriginalFileName(),
                asset.getSizeBytes(), asset.getDurationSeconds(), asset.getCreatedAt());
    }

    private UUID organizationId(UUID userId) {
        return users.findById(userId)
                .orElseThrow(() -> new NotFoundException("User " + userId + " not found"))
                .getOrganization().getId();
    }

    private CoachProfile coach(UUID coachUserId) {
        return coachProfiles.findByUserId(coachUserId)
                .orElseThrow(() -> new NotFoundException("No coach profile for this account"));
    }
}
