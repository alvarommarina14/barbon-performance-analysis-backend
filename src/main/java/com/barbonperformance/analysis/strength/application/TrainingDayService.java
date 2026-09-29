package com.barbonperformance.analysis.strength.application;

import com.barbonperformance.analysis.shared.domain.NotFoundException;
import com.barbonperformance.analysis.strength.application.dto.StrengthDtos.ReplaceTrainingDaysRequest;
import com.barbonperformance.analysis.strength.application.dto.StrengthDtos.TrainingDayInput;
import com.barbonperformance.analysis.strength.application.dto.StrengthDtos.TrainingDayView;
import com.barbonperformance.analysis.strength.domain.DayExercises;
import com.barbonperformance.analysis.strength.domain.TrainingDay;
import com.barbonperformance.analysis.strength.domain.TrainingDays;
import com.barbonperformance.analysis.user.domain.AppUsers;
import com.barbonperformance.analysis.user.domain.CoachProfile;
import com.barbonperformance.analysis.user.domain.CoachProfiles;
import com.barbonperformance.analysis.user.domain.PlayerProfile;
import com.barbonperformance.analysis.user.domain.PlayerProfiles;
import com.barbonperformance.analysis.user.domain.UserRole;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TrainingDayService {

    private final TrainingDays trainingDays;
    private final DayExercises dayExercises;
    private final AppUsers users;
    private final PlayerProfiles playerProfiles;
    private final CoachProfiles coachProfiles;

    public TrainingDayService(TrainingDays trainingDays, DayExercises dayExercises, AppUsers users,
            PlayerProfiles playerProfiles, CoachProfiles coachProfiles) {
        this.trainingDays = trainingDays;
        this.dayExercises = dayExercises;
        this.users = users;
        this.playerProfiles = playerProfiles;
        this.coachProfiles = coachProfiles;
    }

    @Transactional(readOnly = true)
    public List<TrainingDayView> listForCaller(UUID userId, UUID playerProfileId) {
        if (playerProfileId != null) {
            return forPlayer(userId, playerProfileId);
        }
        return forCallerOwnPlayer(userId);
    }

    @Transactional
    public List<TrainingDayView> replaceForPlayer(UUID coachUserId, UUID playerProfileId,
            ReplaceTrainingDaysRequest request) {
        PlayerProfile player = loadOwnedPlayer(coachUserId, playerProfileId);

        trainingDays.deleteAllByPlayerId(player.getId());

        List<TrainingDay> toSave = new ArrayList<>();
        int order = 0;
        for (TrainingDayInput input : request.days()) {
            toSave.add(new TrainingDay(player.getOrganization(), player, input.dayLabel(),
                    input.title(), input.focus(), input.durationMinutes(), input.kind(), order++));
        }

        return trainingDays.saveAll(toSave).stream()
                .map(day -> describe(day, 0))
                .toList();
    }

    private List<TrainingDayView> forPlayer(UUID userId, UUID playerProfileId) {
        if (role(userId) == UserRole.PLAYER) {
            throw new AccessDeniedException("Not entitled to another player's training days");
        }
        PlayerProfile player = playerProfiles.findById(playerProfileId)
                .orElseThrow(() -> new NotFoundException("Player " + playerProfileId + " not found"));
        if (role(userId) == UserRole.COACH
                && (player.getCoach() == null
                        || !player.getCoach().getId().equals(coachProfileId(userId)))) {
            throw new NotFoundException("Player " + playerProfileId + " not found");
        }
        return describeAll(trainingDays.findAllByPlayerId(playerProfileId));
    }

    private List<TrainingDayView> forCallerOwnPlayer(UUID userId) {
        if (role(userId) != UserRole.PLAYER) {
            return List.of();
        }
        UUID playerProfileId = playerProfiles.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("No player profile for this account"))
                .getId();
        return describeAll(trainingDays.findAllByPlayerId(playerProfileId));
    }

    private List<TrainingDayView> describeAll(List<TrainingDay> days) {
        return days.stream()
                .map(day -> describe(day, dayExercises.countByTrainingDayId(day.getId())))
                .toList();
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

    static TrainingDayView describe(TrainingDay day, int exerciseCount) {
        return new TrainingDayView(
                day.getId(), day.getDayLabel(), day.getTitle(), day.getFocus(),
                day.getDurationMinutes(), day.getKind(), day.getSortOrder(), exerciseCount);
    }
}
