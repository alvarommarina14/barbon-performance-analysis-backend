package com.barbonperformance.analysis.strength.application;

import com.barbonperformance.analysis.shared.domain.ConflictException;
import com.barbonperformance.analysis.shared.domain.NotFoundException;
import com.barbonperformance.analysis.strength.application.dto.ExerciseLogDtos.ExerciseLogView;
import com.barbonperformance.analysis.strength.application.dto.ExerciseLogDtos.LogExerciseRequest;
import com.barbonperformance.analysis.strength.application.dto.ExerciseLogDtos.LoggedSetInput;
import com.barbonperformance.analysis.strength.application.dto.ExerciseLogDtos.LoggedSetView;
import com.barbonperformance.analysis.strength.domain.DayExercise;
import com.barbonperformance.analysis.strength.domain.DayExercises;
import com.barbonperformance.analysis.strength.domain.ExerciseLog;
import com.barbonperformance.analysis.strength.domain.ExerciseLogs;
import com.barbonperformance.analysis.strength.domain.LoggedSet;
import com.barbonperformance.analysis.strength.domain.TrainingDay;
import com.barbonperformance.analysis.strength.domain.TrainingDays;
import com.barbonperformance.analysis.user.domain.PlanCalendar;
import com.barbonperformance.analysis.user.domain.PlayerProfile;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExerciseLogService {

    private final ExerciseLogs logs;
    private final DayExercises dayExercises;
    private final TrainingDays trainingDays;
    private final PlayerAccess access;

    public ExerciseLogService(ExerciseLogs logs, DayExercises dayExercises, TrainingDays trainingDays,
            PlayerAccess access) {
        this.logs = logs;
        this.dayExercises = dayExercises;
        this.trainingDays = trainingDays;
        this.access = access;
    }

    @Transactional(readOnly = true)
    public List<ExerciseLogView> listForDay(UUID userId, UUID trainingDayId, int planWeek) {
        requireWeekInPlan(planWeek);
        TrainingDay day = trainingDays.findById(trainingDayId)
                .orElseThrow(() -> new NotFoundException("Training day " + trainingDayId + " not found"));
        PlayerProfile player = access.readableBy(userId, day.getPlayer().getId());
        return logs.findAllForDay(player.getId(), day.getId(), planWeek).stream()
                .map(ExerciseLogService::describe)
                .toList();
    }

    // Only the player logs, only their own exercises, and only weeks they have reached. An empty
    // set list removes the week's log.
    @Transactional
    public Optional<ExerciseLogView> log(UUID playerUserId, UUID dayExerciseId, int planWeek,
            LogExerciseRequest request) {
        requireWeekInPlan(planWeek);
        PlayerProfile player = access.ownPlayer(playerUserId);
        DayExercise exercise = dayExercises.findById(dayExerciseId)
                .filter(e -> e.getTrainingDay().getPlayer().getId().equals(player.getId()))
                .orElseThrow(() -> new NotFoundException("Exercise " + dayExerciseId + " not found"));
        if (planWeek > PlanCalendar.unlockedWeeks(player)) {
            throw new ConflictException("week " + planWeek + " has not started yet");
        }

        Optional<ExerciseLog> existing = logs.findOne(player.getId(), exercise.getId(), planWeek);
        if (request.sets().isEmpty()) {
            existing.ifPresent(logs::delete);
            return Optional.empty();
        }

        List<LoggedSet> sets = new ArrayList<>();
        int setNumber = 1;
        for (LoggedSetInput input : request.sets()) {
            sets.add(new LoggedSet(setNumber++, input.reps(), input.weight()));
        }
        ExerciseLog log = existing.orElseGet(() -> new ExerciseLog(player, exercise, planWeek));
        log.record(request.unit(), sets);
        return Optional.of(describe(logs.save(log)));
    }

    private static void requireWeekInPlan(int planWeek) {
        if (planWeek < 1 || planWeek > PlanCalendar.PLAN_WEEKS) {
            throw new NotFoundException("Plan week " + planWeek + " does not exist");
        }
    }

    private static ExerciseLogView describe(ExerciseLog log) {
        return new ExerciseLogView(
                log.getDayExercise().getId(),
                log.getPlanWeek(),
                log.getUnit(),
                log.getLoggedAt(),
                log.getSets().stream()
                        .map(set -> new LoggedSetView(set.getSetNumber(), set.getReps(), set.getWeight()))
                        .toList());
    }
}
