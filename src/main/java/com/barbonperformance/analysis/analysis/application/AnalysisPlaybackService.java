package com.barbonperformance.analysis.analysis.application;

import com.barbonperformance.analysis.analysis.application.dto.AnalysisDtos.PlaybackView;
import com.barbonperformance.analysis.analysis.domain.Analyses;
import com.barbonperformance.analysis.analysis.domain.Analysis;
import com.barbonperformance.analysis.analysis.domain.AnalysisStatus;
import com.barbonperformance.analysis.shared.domain.NotFoundException;
import com.barbonperformance.analysis.user.domain.AppUsers;
import com.barbonperformance.analysis.user.domain.CoachProfiles;
import com.barbonperformance.analysis.user.domain.PlayerProfiles;
import com.barbonperformance.analysis.user.domain.UserRole;
import com.barbonperformance.analysis.video.VideoProperties;
import com.barbonperformance.analysis.video.domain.StreamGateway;
import com.barbonperformance.analysis.video.domain.VideoAsset;
import java.time.Instant;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalysisPlaybackService {

    private final Analyses analyses;
    private final AppUsers users;
    private final PlayerProfiles playerProfiles;
    private final CoachProfiles coachProfiles;
    private final StreamGateway stream;
    private final VideoProperties properties;

    public AnalysisPlaybackService(Analyses analyses, AppUsers users, PlayerProfiles playerProfiles,
            CoachProfiles coachProfiles, StreamGateway stream, VideoProperties properties) {
        this.analyses = analyses;
        this.users = users;
        this.playerProfiles = playerProfiles;
        this.coachProfiles = coachProfiles;
        this.stream = stream;
        this.properties = properties;
    }

    // A missing analysis is a 403, not a 404: the id space must not be probeable. The player
    // only reaches a published analysis of their own; the coach can preview their own draft.
    @Transactional(readOnly = true)
    public PlaybackView playback(UUID userId, UUID analysisId) {
        Analysis analysis = analyses.findDetailedById(analysisId)
                .orElseThrow(() -> new AccessDeniedException("Not entitled to this video"));

        if (!isEntitled(userId, analysis)) {
            throw new AccessDeniedException("Not entitled to this video");
        }

        VideoAsset asset = analysis.getFeedbackVideoAsset();
        if (asset == null || !asset.isReady()) {
            throw new NotFoundException("That feedback video is not ready yet");
        }

        String token = stream.signPlayback(asset.getStreamUid(), properties.playbackTokenTtl());
        return new PlaybackView(
                asset.getId(),
                "https://videodelivery.net/" + token + "/manifest/video.m3u8",
                "https://videodelivery.net/" + token + "/manifest/video.mpd",
                token,
                Instant.now().plus(properties.playbackTokenTtl()));
    }

    private boolean isEntitled(UUID userId, Analysis analysis) {
        UserRole role = users.findById(userId)
                .orElseThrow(() -> new NotFoundException("User " + userId + " not found"))
                .getRole();

        return switch (role) {
            case ADMIN -> true;
            case PLAYER -> analysis.getStatus() == AnalysisStatus.PUBLISHED
                    && playerProfiles.findByUserId(userId)
                            .map(profile -> profile.getId().equals(analysis.getPlayer().getId()))
                            .orElse(false);
            case COACH -> coachProfiles.findByUserId(userId)
                    .map(coach -> coach.getId().equals(analysis.getCoach().getId()))
                    .orElse(false);
        };
    }
}
