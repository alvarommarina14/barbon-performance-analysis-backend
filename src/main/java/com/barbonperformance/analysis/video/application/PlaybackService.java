package com.barbonperformance.analysis.video.application;

import com.barbonperformance.analysis.shared.domain.NotFoundException;
import com.barbonperformance.analysis.user.domain.AppUsers;
import com.barbonperformance.analysis.user.domain.CoachProfiles;
import com.barbonperformance.analysis.user.domain.PlayerProfiles;
import com.barbonperformance.analysis.user.domain.UserRole;
import com.barbonperformance.analysis.video.VideoProperties;
import com.barbonperformance.analysis.video.application.dto.VideoDtos.DownloadUrlResponse;
import com.barbonperformance.analysis.video.application.dto.VideoDtos.PlaybackResponse;
import com.barbonperformance.analysis.video.domain.StreamGateway;
import com.barbonperformance.analysis.video.domain.VideoAsset;
import com.barbonperformance.analysis.video.domain.VideoAssets;
import com.barbonperformance.analysis.video.domain.VideoSubmission;
import com.barbonperformance.analysis.video.domain.VideoSubmissions;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlaybackService {

    private final VideoAssets videoAssets;
    private final VideoSubmissions submissions;
    private final PlayerProfiles playerProfiles;
    private final CoachProfiles coachProfiles;
    private final AppUsers users;
    private final StreamGateway stream;
    private final VideoProperties properties;

    public PlaybackService(VideoAssets videoAssets, VideoSubmissions submissions,
            PlayerProfiles playerProfiles, CoachProfiles coachProfiles, AppUsers users,
            StreamGateway stream, VideoProperties properties) {
        this.videoAssets = videoAssets;
        this.submissions = submissions;
        this.playerProfiles = playerProfiles;
        this.coachProfiles = coachProfiles;
        this.users = users;
        this.stream = stream;
        this.properties = properties;
    }

    // No playback id is ever served unprotected, and the entitlement check runs before the
    // token is minted rather than after: a player must own the footage, a coach must own the
    // player. Anything else is a 403, including a video that simply does not exist, so the
    // endpoint cannot be used to probe for asset ids.
    @Transactional(readOnly = true)
    public PlaybackResponse playback(UUID userId, UUID videoAssetId) {
        VideoAsset asset = videoAssets.findById(videoAssetId)
                .orElseThrow(() -> new AccessDeniedException("Not entitled to this video"));

        if (!asset.isReady()) {
            throw new NotFoundException("That video is not ready yet");
        }
        if (!isEntitled(userId, asset)) {
            throw new AccessDeniedException("Not entitled to this video");
        }

        StreamGateway.Playback playback =
                stream.signPlayback(asset.getStreamUid(), properties.playbackTokenTtl());
        return new PlaybackResponse(
                asset.getId(),
                playback.hlsUrl(),
                playback.dashUrl(),
                playback.token(),
                playback.expiresAt());
    }

    // The coach edits the feedback video outside the app, so they need the original MP4. Same
    // entitlement as playback; Cloudflare renders the download lazily, so the response says
    // whether the url is usable yet and the client polls otherwise.
    @Transactional(readOnly = true)
    public DownloadUrlResponse downloadUrl(UUID userId, UUID videoAssetId) {
        VideoAsset asset = videoAssets.findById(videoAssetId)
                .orElseThrow(() -> new AccessDeniedException("Not entitled to this video"));

        if (!asset.isReady()) {
            throw new NotFoundException("That video is not ready yet");
        }
        if (!isEntitled(userId, asset)) {
            throw new AccessDeniedException("Not entitled to this video");
        }

        StreamGateway.DownloadResult download = stream.enableDownload(asset.getStreamUid());
        return new DownloadUrlResponse(download.url(), download.ready());
    }

    private boolean isEntitled(UUID userId, VideoAsset asset) {
        UserRole role = users.findById(userId)
                .orElseThrow(() -> new NotFoundException("User " + userId + " not found"))
                .getRole();

        if (role == UserRole.ADMIN) {
            return true;
        }

        VideoSubmission submission = submissions.findByAssetId(asset.getId()).orElse(null);
        if (submission == null) {
            return false;
        }

        return switch (role) {
            case PLAYER -> playerProfiles.findByUserId(userId)
                    .map(profile -> profile.getId().equals(submission.getPlayer().getId()))
                    .orElse(false);
            case COACH -> coachProfiles.findByUserId(userId)
                    .map(coach -> submission.getPlayer().getCoach() != null
                            && coach.getId().equals(submission.getPlayer().getCoach().getId()))
                    .orElse(false);
            default -> false;
        };
    }
}
