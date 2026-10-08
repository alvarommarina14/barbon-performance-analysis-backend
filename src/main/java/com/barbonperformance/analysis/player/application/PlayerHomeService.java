package com.barbonperformance.analysis.player.application;

import com.barbonperformance.analysis.player.application.dto.PlayerHomeDtos.CoachCard;
import com.barbonperformance.analysis.player.application.dto.PlayerHomeDtos.PhaseCard;
import com.barbonperformance.analysis.player.application.dto.PlayerHomeDtos.PlayerHomeResponse;
import com.barbonperformance.analysis.shared.domain.NotFoundException;
import com.barbonperformance.analysis.strength.domain.CyclePhase;
import com.barbonperformance.analysis.strength.StrengthProperties;
import com.barbonperformance.analysis.strength.domain.CyclePhases;
import com.barbonperformance.analysis.strength.domain.ProgramLinks;
import com.barbonperformance.analysis.strength.domain.TrainingDays;
import com.barbonperformance.analysis.user.domain.CoachProfile;
import com.barbonperformance.analysis.user.domain.PlanCalendar;
import com.barbonperformance.analysis.user.domain.PlayerProfile;
import com.barbonperformance.analysis.user.domain.PlayerProfiles;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlayerHomeService {

    private final PlayerProfiles playerProfiles;
    private final CyclePhases cyclePhases;
    private final TrainingDays trainingDays;
    private final ProgramLinks programLinks;
    private final StrengthProperties strengthProperties;

    public PlayerHomeService(PlayerProfiles playerProfiles, CyclePhases cyclePhases, TrainingDays trainingDays,
            ProgramLinks programLinks, StrengthProperties strengthProperties) {
        this.playerProfiles = playerProfiles;
        this.cyclePhases = cyclePhases;
        this.trainingDays = trainingDays;
        this.programLinks = programLinks;
        this.strengthProperties = strengthProperties;
    }

    @Transactional(readOnly = true)
    public PlayerHomeResponse home(UUID userId) {
        PlayerProfile profile = playerProfiles.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("No player profile for this account"));

        CoachProfile coach = profile.getCoach();
        Integer planWeek = PlanCalendar.planWeek(profile).orElse(null);
        Integer cycleWeek = planWeek == null ? null : PlanCalendar.cycleWeek(planWeek);
        PhaseCard currentPhase = cycleWeek == null ? null
                : cyclePhases.findByPlayerIdAndWeekNumber(profile.getId(), cycleWeek)
                        .map(PlayerHomeService::describe)
                        .orElse(null);
        // The PDF is generated from the program, so there is one to open as soon as there are days.
        String programPdfUrl = trainingDays.findAllByPlayerId(profile.getId()).isEmpty() ? null
                : programLinks.programPdf(profile.getId(), Instant.now().plus(strengthProperties.programPdfLinkTtl()));

        return new PlayerHomeResponse(
                profile.getId(),
                profile.getUser().getName(),
                profile.getUser().getAvatarUrl(),
                profile.getMembershipTier(),
                planWeek,
                PlanCalendar.PLAN_WEEKS,
                cycleWeek,
                profile.getPlanStartedAt(),
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
                phase.getWeekNumber(), phase.getPhaseName(), phase.getGoal(),
                phase.getRpeRange());
    }
}
