package com.barbonperformance.analysis.strength.application;

import com.barbonperformance.analysis.shared.domain.ConflictException;
import com.barbonperformance.analysis.shared.domain.NotFoundException;
import com.barbonperformance.analysis.strength.application.dto.StrengthDtos.ApplyCyclePhaseTemplateRequest;
import com.barbonperformance.analysis.strength.application.dto.StrengthDtos.CyclePhaseInput;
import com.barbonperformance.analysis.strength.application.dto.StrengthDtos.CyclePhaseView;
import com.barbonperformance.analysis.strength.application.dto.StrengthDtos.ReplaceCyclePhasesRequest;
import com.barbonperformance.analysis.strength.domain.CyclePhase;
import com.barbonperformance.analysis.strength.domain.CyclePhases;
import com.barbonperformance.analysis.user.domain.AppUsers;
import com.barbonperformance.analysis.user.domain.CoachProfile;
import com.barbonperformance.analysis.user.domain.CoachProfiles;
import com.barbonperformance.analysis.user.domain.PlayerProfile;
import com.barbonperformance.analysis.user.domain.PlayerProfiles;
import com.barbonperformance.analysis.user.domain.UserRole;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CyclePhaseService {

    private static final Set<Integer> REQUIRED_WEEKS = Set.of(1, 2, 3, 4);

    private final CyclePhases cyclePhases;
    private final AppUsers users;
    private final PlayerProfiles playerProfiles;
    private final CoachProfiles coachProfiles;

    public CyclePhaseService(CyclePhases cyclePhases, AppUsers users, PlayerProfiles playerProfiles,
            CoachProfiles coachProfiles) {
        this.cyclePhases = cyclePhases;
        this.users = users;
        this.playerProfiles = playerProfiles;
        this.coachProfiles = coachProfiles;
    }

    @Transactional(readOnly = true)
    public List<CyclePhaseView> listForCaller(UUID userId, UUID playerProfileId) {
        if (playerProfileId != null) {
            return forPlayer(userId, playerProfileId);
        }
        return forCallerOwnPlayer(userId);
    }

    @Transactional
    public List<CyclePhaseView> replaceForPlayer(UUID coachUserId, UUID playerProfileId,
            ReplaceCyclePhasesRequest request) {
        PlayerProfile player = loadOwnedPlayer(coachUserId, playerProfileId);
        requireExactlyRequiredWeeks(request.phases().stream().map(CyclePhaseInput::weekNumber));

        List<CyclePhase> toSave = new ArrayList<>();
        for (CyclePhaseInput input : request.phases()) {
            toSave.add(upsertPlayerWeek(player, input.weekNumber(), input.phaseName(), input.goal(),
                    input.setsReps(), input.rpeRange()));
        }
        return save(toSave);
    }

    @Transactional
    public List<CyclePhaseView> applyTemplateToPlayer(UUID coachUserId, UUID playerProfileId,
            ApplyCyclePhaseTemplateRequest request) {
        PlayerProfile player = loadOwnedPlayer(coachUserId, playerProfileId);

        List<CyclePhase> templateWeeks = cyclePhases.findAllByOrgIdAndTemplateName(
                player.getOrganization().getId(), request.templateName());
        if (templateWeeks.isEmpty()) {
            throw new NotFoundException("Template \"" + request.templateName() + "\" not found");
        }

        List<CyclePhase> toSave = new ArrayList<>();
        for (CyclePhase templateWeek : templateWeeks) {
            toSave.add(upsertPlayerWeek(player, templateWeek.getWeekNumber(), templateWeek.getPhaseName(),
                    templateWeek.getGoal(), templateWeek.getSetsReps(), templateWeek.getRpeRange()));
        }
        return save(toSave);
    }

    private CyclePhase upsertPlayerWeek(PlayerProfile player, int weekNumber, String phaseName, String goal,
            String setsReps, String rpeRange) {
        return cyclePhases.findByPlayerIdAndWeekNumber(player.getId(), weekNumber)
                .map(existing -> {
                    existing.updateDetails(phaseName, goal, setsReps, rpeRange);
                    return existing;
                })
                .orElseGet(() -> CyclePhase.forPlayer(player.getOrganization(), player, weekNumber, phaseName,
                        goal, setsReps, rpeRange));
    }

    private List<CyclePhaseView> save(List<CyclePhase> toSave) {
        return cyclePhases.saveAll(toSave).stream()
                .sorted((a, b) -> Integer.compare(a.getWeekNumber(), b.getWeekNumber()))
                .map(CyclePhaseService::describe)
                .toList();
    }

    private void requireExactlyRequiredWeeks(Stream<Integer> weekNumbers) {
        Set<Integer> weeks = weekNumbers.collect(Collectors.toSet());
        if (!weeks.equals(REQUIRED_WEEKS)) {
            throw new ConflictException("the cycle needs exactly weeks 1 through 4, once each");
        }
    }

    private List<CyclePhaseView> forPlayer(UUID userId, UUID playerProfileId) {
        if (role(userId) == UserRole.PLAYER) {
            throw new AccessDeniedException("Not entitled to another player's cycle phases");
        }
        PlayerProfile player = playerProfiles.findById(playerProfileId)
                .orElseThrow(() -> new NotFoundException("Player " + playerProfileId + " not found"));
        if (role(userId) == UserRole.COACH
                && (player.getCoach() == null
                        || !player.getCoach().getId().equals(coachProfileId(userId)))) {
            throw new NotFoundException("Player " + playerProfileId + " not found");
        }
        return describeAll(cyclePhases.findAllByPlayerId(playerProfileId));
    }

    private List<CyclePhaseView> forCallerOwnPlayer(UUID userId) {
        if (role(userId) != UserRole.PLAYER) {
            return List.of();
        }
        UUID playerProfileId = playerProfiles.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("No player profile for this account"))
                .getId();
        return describeAll(cyclePhases.findAllByPlayerId(playerProfileId));
    }

    private List<CyclePhaseView> describeAll(List<CyclePhase> phases) {
        return phases.stream().map(CyclePhaseService::describe).toList();
    }

    private PlayerProfile loadOwnedPlayer(UUID coachUserId, UUID playerProfileId) {
        CoachProfile coach = coach(coachUserId);
        PlayerProfile player = playerProfiles.findById(playerProfileId)
                .orElseThrow(() -> new NotFoundException("Player " + playerProfileId + " not found"));
        if (player.getCoach() == null || !player.getCoach().getId().equals(coach.getId())) {
            throw new NotFoundException("Player " + playerProfileId + " not found");
        }
        return player;
    }

    private UserRole role(UUID userId) {
        return users.findById(userId)
                .orElseThrow(() -> new NotFoundException("User " + userId + " not found"))
                .getRole();
    }

    private UUID coachProfileId(UUID userId) {
        return coach(userId).getId();
    }

    private CoachProfile coach(UUID coachUserId) {
        return coachProfiles.findByUserId(coachUserId)
                .orElseThrow(() -> new NotFoundException("No coach profile for this account"));
    }

    static CyclePhaseView describe(CyclePhase phase) {
        return new CyclePhaseView(
                phase.getId(), phase.getWeekNumber(), phase.getPhaseName(), phase.getGoal(),
                phase.getSetsReps(), phase.getRpeRange());
    }
}
