package com.barbonperformance.analysis.player.application;

import com.barbonperformance.analysis.content.ContentProperties;
import com.barbonperformance.analysis.content.domain.ObjectStorage;
import com.barbonperformance.analysis.player.application.dto.PlayerHomeDtos.CoachCard;
import com.barbonperformance.analysis.player.application.dto.PlayerHomeDtos.PhaseCard;
import com.barbonperformance.analysis.player.application.dto.PlayerHomeDtos.PlayerHomeResponse;
import com.barbonperformance.analysis.shared.domain.NotFoundException;
import com.barbonperformance.analysis.strength.domain.CyclePhase;
import com.barbonperformance.analysis.strength.domain.CyclePhases;
import com.barbonperformance.analysis.user.domain.CoachProfile;
import com.barbonperformance.analysis.user.domain.PlayerProfile;
import com.barbonperformance.analysis.user.domain.PlayerProfiles;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlayerHomeService {

    private final PlayerProfiles playerProfiles;
    private final CyclePhases cyclePhases;
    private final ObjectStorage objectStorage;
    private final ContentProperties contentProperties;

    public PlayerHomeService(PlayerProfiles playerProfiles, CyclePhases cyclePhases,
            ObjectStorage objectStorage, ContentProperties contentProperties) {
        this.playerProfiles = playerProfiles;
        this.cyclePhases = cyclePhases;
        this.objectStorage = objectStorage;
        this.contentProperties = contentProperties;
    }

    @Transactional(readOnly = true)
    public PlayerHomeResponse home(UUID userId) {
        PlayerProfile profile = playerProfiles.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("No player profile for this account"));

        CoachProfile coach = profile.getCoach();
        PhaseCard currentPhase = cyclePhases
                .findByPlayerIdAndWeekNumber(profile.getId(), profile.getCurrentWeek())
                .map(PlayerHomeService::describe)
                .orElse(null);
        String programPdfUrl = profile.getProgramPdfKey() == null ? null
                : objectStorage.presignDownload(profile.getProgramPdfKey(), contentProperties.presignTtl());

        return new PlayerHomeResponse(
                profile.getId(),
                profile.getUser().getName(),
                profile.getUser().getAvatarUrl(),
                profile.getMembershipTier(),
                profile.getCurrentWeek(),
                PlayerProfile.CYCLE_WEEKS,
                profile.getCycleStartedAt(),
                profile.getNextCallAt(),
                coach == null ? null : new CoachCard(
                        coach.getId(),
                        coach.getUser().getName(),
                        coach.getUser().getAvatarUrl(),
                        coach.getBookingUrl()),
                currentPhase,
                programPdfUrl);
    }

    private static PhaseCard describe(CyclePhase phase) {
        return new PhaseCard(
                phase.getWeekNumber(), phase.getPhaseName(), phase.getGoal(), phase.getSetsReps(),
                phase.getRpeRange());
    }
}
