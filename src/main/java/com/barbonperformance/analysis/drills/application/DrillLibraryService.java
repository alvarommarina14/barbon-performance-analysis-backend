package com.barbonperformance.analysis.drills.application;

import com.barbonperformance.analysis.drills.application.dto.DrillDtos.CreateLibraryItemRequest;
import com.barbonperformance.analysis.drills.application.dto.DrillDtos.DrillPlaybackView;
import com.barbonperformance.analysis.drills.application.dto.DrillDtos.LibraryItemView;
import com.barbonperformance.analysis.drills.application.dto.DrillDtos.LibraryUploadIntentRequest;
import com.barbonperformance.analysis.drills.application.dto.DrillDtos.LibraryUploadIntentResponse;
import com.barbonperformance.analysis.drills.application.dto.DrillDtos.UpdateLibraryItemRequest;
import com.barbonperformance.analysis.drills.domain.DrillAssignments;
import com.barbonperformance.analysis.drills.domain.LibraryItem;
import com.barbonperformance.analysis.drills.domain.LibraryItems;
import com.barbonperformance.analysis.shared.domain.ConflictException;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DrillLibraryService {

    private final AppUsers users;
    private final CoachProfiles coachProfiles;
    private final LibraryItems libraryItems;
    private final DrillAssignments drillAssignments;
    private final VideoAssets videoAssets;
    private final StreamGateway stream;
    private final VideoProperties properties;

    public DrillLibraryService(AppUsers users, CoachProfiles coachProfiles, LibraryItems libraryItems,
            DrillAssignments drillAssignments, VideoAssets videoAssets, StreamGateway stream,
            VideoProperties properties) {
        this.users = users;
        this.coachProfiles = coachProfiles;
        this.libraryItems = libraryItems;
        this.drillAssignments = drillAssignments;
        this.videoAssets = videoAssets;
        this.stream = stream;
        this.properties = properties;
    }

    @Transactional
    public LibraryUploadIntentResponse requestUploadIntent(UUID coachUserId,
            LibraryUploadIntentRequest request) {
        CoachProfile coach = coach(coachUserId);

        VideoAsset asset = videoAssets.save(
                new VideoAsset(coach.getOrganization(), VideoAssetKind.LIBRARY_DRILL));

        StreamGateway.DirectUpload upload = stream.createDirectUpload(
                new StreamGateway.DirectUploadRequest(
                        coach.getId().toString(),
                        properties.maxDurationSeconds(),
                        request.sizeBytes(),
                        Map.of("videoAssetId", asset.getId().toString())));

        asset.attachToStream(upload.streamUid());

        return new LibraryUploadIntentResponse(
                asset.getId(), upload.uploadUrl(), properties.maxDurationSeconds());
    }

    @Transactional
    public LibraryItemView createLibraryItem(UUID coachUserId, CreateLibraryItemRequest request) {
        CoachProfile coach = coach(coachUserId);

        VideoAsset asset = videoAssets.findById(request.videoAssetId())
                .filter(a -> a.getKind() == VideoAssetKind.LIBRARY_DRILL)
                .filter(a -> a.getOrganization().getId().equals(coach.getOrganization().getId()))
                .orElseThrow(() -> new NotFoundException(
                        "Video " + request.videoAssetId() + " not found"));
        if (!asset.isReady()) {
            throw new VideoNotReadyException("the video is still uploading");
        }

        LibraryItem item = libraryItems.save(new LibraryItem(
                coach.getOrganization(), request.title(), request.cue(), request.category(), asset));
        return describe(item);
    }

    @Transactional
    public LibraryItemView update(UUID coachUserId, UUID itemId, UpdateLibraryItemRequest request) {
        LibraryItem item = loadOwned(coachUserId, itemId);
        item.updateDetails(request.title(), request.cue(), request.category());
        return describe(item);
    }

    @Transactional
    public void delete(UUID coachUserId, UUID itemId) {
        LibraryItem item = loadOwned(coachUserId, itemId);
        if (drillAssignments.existsByLibraryItemId(item.getId())) {
            throw new ConflictException("that drill is still assigned to a player");
        }
        libraryItems.delete(item);
    }

    @Transactional(readOnly = true)
    public List<LibraryItemView> list(UUID userId) {
        return libraryItems.findAllByOrgId(organizationId(userId)).stream()
                .map(DrillLibraryService::describe)
                .toList();
    }

    @Transactional(readOnly = true)
    public DrillPlaybackView playback(UUID userId, UUID itemId) {
        UUID orgId = organizationId(userId);
        LibraryItem item = libraryItems.findById(itemId)
                .filter(i -> i.getOrganization().getId().equals(orgId))
                .orElseThrow(() -> new NotFoundException("Drill " + itemId + " not found"));

        VideoAsset asset = item.getVideoAsset();
        StreamGateway.Playback playback =
                stream.signPlayback(asset.getStreamUid(), properties.playbackTokenTtl());
        return new DrillPlaybackView(
                asset.getId(), playback.hlsUrl(), playback.dashUrl(), playback.token(),
                playback.expiresAt());
    }

    private CoachProfile coach(UUID coachUserId) {
        return coachProfiles.findByUserId(coachUserId)
                .orElseThrow(() -> new NotFoundException("No coach profile for this account"));
    }

    private UUID organizationId(UUID userId) {
        return users.findById(userId)
                .orElseThrow(() -> new NotFoundException("User " + userId + " not found"))
                .getOrganization().getId();
    }

    // A drill from another org is reported as missing rather than forbidden, same convention as
    // AnalysisService.loadOwned.
    private LibraryItem loadOwned(UUID coachUserId, UUID itemId) {
        CoachProfile coach = coach(coachUserId);
        LibraryItem item = libraryItems.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Drill " + itemId + " not found"));
        if (!item.getOrganization().getId().equals(coach.getOrganization().getId())) {
            throw new NotFoundException("Drill " + itemId + " not found");
        }
        return item;
    }

    static LibraryItemView describe(LibraryItem item) {
        VideoAsset asset = item.getVideoAsset();
        return new LibraryItemView(
                item.getId(),
                item.getTitle(),
                item.getCue(),
                item.getCategory(),
                asset.getId(),
                asset.getStatus(),
                asset.getDurationSeconds(),
                asset.getThumbnailUrl(),
                item.getCreatedAt());
    }
}
