package com.barbonperformance.analysis.strength.application;

import com.barbonperformance.analysis.drills.domain.LibraryItem;
import com.barbonperformance.analysis.drills.domain.LibraryItems;
import com.barbonperformance.analysis.shared.domain.NotFoundException;
import com.barbonperformance.analysis.strength.application.dto.StrengthDtos.DayExerciseInput;
import com.barbonperformance.analysis.strength.application.dto.StrengthDtos.DayExerciseView;
import com.barbonperformance.analysis.strength.application.dto.StrengthDtos.ReplaceDayExercisesRequest;
import com.barbonperformance.analysis.strength.domain.DayExercise;
import com.barbonperformance.analysis.strength.domain.DayExercises;
import com.barbonperformance.analysis.strength.domain.TrainingDay;
import com.barbonperformance.analysis.strength.domain.TrainingDays;
import com.barbonperformance.analysis.user.domain.AppUsers;
import com.barbonperformance.analysis.user.domain.CoachProfile;
import com.barbonperformance.analysis.user.domain.CoachProfiles;
import com.barbonperformance.analysis.user.domain.UserRole;
import com.barbonperformance.analysis.video.domain.VideoAsset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DayExerciseService {

    private final DayExercises dayExercises;
    private final TrainingDays trainingDays;
    private final LibraryItems libraryItems;
    private final AppUsers users;
    private final CoachProfiles coachProfiles;

    public DayExerciseService(DayExercises dayExercises, TrainingDays trainingDays,
            LibraryItems libraryItems, AppUsers users, CoachProfiles coachProfiles) {
        this.dayExercises = dayExercises;
        this.trainingDays = trainingDays;
        this.libraryItems = libraryItems;
        this.users = users;
        this.coachProfiles = coachProfiles;
    }

    @Transactional(readOnly = true)
    public List<DayExerciseView> listForCaller(UUID userId, UUID trainingDayId) {
        TrainingDay day = loadVisibleDay(userId, trainingDayId);
        return dayExercises.findAllByTrainingDayId(day.getId()).stream()
                .map(DayExerciseService::describe)
                .toList();
    }

    @Transactional
    public List<DayExerciseView> replaceForDay(UUID coachUserId, UUID trainingDayId,
            ReplaceDayExercisesRequest request) {
        CoachProfile coach = coach(coachUserId);
        TrainingDay day = trainingDays.findById(trainingDayId)
                .orElseThrow(() -> new NotFoundException("Training day " + trainingDayId + " not found"));
        if (day.getPlayer().getCoach() == null || !day.getPlayer().getCoach().getId().equals(coach.getId())) {
            throw new NotFoundException("Training day " + trainingDayId + " not found");
        }

        Map<UUID, DayExercise> existing = dayExercises.findAllByTrainingDayId(day.getId()).stream()
                .collect(Collectors.toMap(DayExercise::getId, Function.identity()));

        List<DayExercise> toSave = new ArrayList<>();
        int order = 0;
        for (DayExerciseInput input : request.exercises()) {
            LibraryItem item = libraryItems.findById(input.libraryItemId())
                    .filter(i -> i.getOrganization().getId().equals(coach.getOrganization().getId()))
                    .orElseThrow(() -> new NotFoundException(
                            "Exercise " + input.libraryItemId() + " not found"));
            if (input.id() == null) {
                toSave.add(new DayExercise(coach.getOrganization(), day, item,
                        input.setsRepsWk1(), input.setsRepsWk2(), input.setsRepsWk3(), input.setsRepsWk4(),
                        input.cueOverride(), order++));
                continue;
            }
            DayExercise exercise = existing.remove(input.id());
            if (exercise == null) {
                throw new NotFoundException("Day exercise " + input.id() + " not found");
            }
            exercise.revise(item, input.setsRepsWk1(), input.setsRepsWk2(), input.setsRepsWk3(),
                    input.setsRepsWk4(), input.cueOverride(), order++);
            toSave.add(exercise);
        }
        dayExercises.deleteAll(List.copyOf(existing.values()));

        return dayExercises.saveAll(toSave).stream()
                .map(DayExerciseService::describe)
                .toList();
    }

    private TrainingDay loadVisibleDay(UUID userId, UUID trainingDayId) {
        TrainingDay day = trainingDays.findById(trainingDayId)
                .orElseThrow(() -> new NotFoundException("Training day " + trainingDayId + " not found"));

        UserRole role = users.findById(userId)
                .orElseThrow(() -> new NotFoundException("User " + userId + " not found"))
                .getRole();
        switch (role) {
            case PLAYER -> {
                if (day.getPlayer().getUser() == null || !day.getPlayer().getUser().getId().equals(userId)) {
                    throw new NotFoundException("Training day " + trainingDayId + " not found");
                }
            }
            case COACH -> {
                CoachProfile coach = coach(userId);
                if (day.getPlayer().getCoach() == null
                        || !day.getPlayer().getCoach().getId().equals(coach.getId())) {
                    throw new NotFoundException("Training day " + trainingDayId + " not found");
                }
            }
            case ADMIN -> {
                // An admin may read any player's training day.
            }
        }
        return day;
    }

    private CoachProfile coach(UUID coachUserId) {
        return coachProfiles.findByUserId(coachUserId)
                .orElseThrow(() -> new NotFoundException("No coach profile for this account"));
    }

    static DayExerciseView describe(DayExercise exercise) {
        LibraryItem item = exercise.getLibraryItem();
        VideoAsset asset = item.getVideoAsset();
        return new DayExerciseView(
                exercise.getId(),
                item.getId(),
                item.getTitle(),
                item.getCue(),
                item.getCategory(),
                asset.getId(),
                asset.getStatus(),
                asset.getDurationSeconds(),
                asset.getThumbnailUrl(),
                exercise.getCueOverride(),
                exercise.getSetsRepsWk1(),
                exercise.getSetsRepsWk2(),
                exercise.getSetsRepsWk3(),
                exercise.getSetsRepsWk4(),
                exercise.getSortOrder());
    }
}
