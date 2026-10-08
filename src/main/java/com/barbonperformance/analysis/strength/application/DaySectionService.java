package com.barbonperformance.analysis.strength.application;

import com.barbonperformance.analysis.drills.domain.LibraryItem;
import com.barbonperformance.analysis.drills.domain.LibraryItems;
import com.barbonperformance.analysis.shared.domain.NotFoundException;
import com.barbonperformance.analysis.strength.application.dto.StrengthDtos.DayExerciseInput;
import com.barbonperformance.analysis.strength.application.dto.StrengthDtos.DayExerciseView;
import com.barbonperformance.analysis.strength.application.dto.StrengthDtos.DaySectionInput;
import com.barbonperformance.analysis.strength.application.dto.StrengthDtos.DaySectionView;
import com.barbonperformance.analysis.strength.application.dto.StrengthDtos.ReplaceDaySectionsRequest;
import com.barbonperformance.analysis.strength.domain.DayExercise;
import com.barbonperformance.analysis.strength.domain.DayExercises;
import com.barbonperformance.analysis.strength.domain.DaySection;
import com.barbonperformance.analysis.strength.domain.DaySections;
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
public class DaySectionService {

    private final DaySections daySections;
    private final DayExercises dayExercises;
    private final TrainingDays trainingDays;
    private final LibraryItems libraryItems;
    private final AppUsers users;
    private final CoachProfiles coachProfiles;

    public DaySectionService(DaySections daySections, DayExercises dayExercises, TrainingDays trainingDays,
            LibraryItems libraryItems, AppUsers users, CoachProfiles coachProfiles) {
        this.daySections = daySections;
        this.dayExercises = dayExercises;
        this.trainingDays = trainingDays;
        this.libraryItems = libraryItems;
        this.users = users;
        this.coachProfiles = coachProfiles;
    }

    @Transactional(readOnly = true)
    public List<DaySectionView> listForCaller(UUID userId, UUID trainingDayId) {
        TrainingDay day = loadVisibleDay(userId, trainingDayId);
        return describeDay(day.getId());
    }

    // Sections and exercises that carry an id are updated in place — an exercise may move to another
    // section — so the player's logs against them survive; anything left out is deleted.
    @Transactional
    public List<DaySectionView> replaceForDay(UUID coachUserId, UUID trainingDayId,
            ReplaceDaySectionsRequest request) {
        CoachProfile coach = coach(coachUserId);
        TrainingDay day = trainingDays.findById(trainingDayId)
                .orElseThrow(() -> new NotFoundException("Training day " + trainingDayId + " not found"));
        if (!editableBy(day, coach)) {
            throw new NotFoundException("Training day " + trainingDayId + " not found");
        }

        Map<UUID, DaySection> existingSections = daySections.findAllByTrainingDayId(day.getId()).stream()
                .collect(Collectors.toMap(DaySection::getId, Function.identity()));
        Map<UUID, DayExercise> existingExercises = dayExercises.findAllByTrainingDayId(day.getId()).stream()
                .collect(Collectors.toMap(DayExercise::getId, Function.identity()));

        List<DaySection> sections = new ArrayList<>();
        int sectionOrder = 0;
        for (DaySectionInput input : request.sections()) {
            String title = input.title().strip();
            String note = blankToNull(input.note());
            if (input.id() == null) {
                sections.add(new DaySection(day, title, note, input.variesByWeek(), sectionOrder++));
                continue;
            }
            DaySection section = existingSections.remove(input.id());
            if (section == null) {
                throw new NotFoundException("Day section " + input.id() + " not found");
            }
            section.revise(title, note, input.variesByWeek(), sectionOrder++);
            sections.add(section);
        }
        sections = daySections.saveAll(sections);

        List<DayExercise> exercises = new ArrayList<>();
        int exerciseOrder = 0;
        for (int i = 0; i < sections.size(); i++) {
            DaySection section = sections.get(i);
            for (DayExerciseInput input : request.sections().get(i).exercises()) {
                LibraryItem item = libraryItems.findById(input.libraryItemId())
                        .filter(found -> found.getOrganization().getId().equals(coach.getOrganization().getId()))
                        .orElseThrow(() -> new NotFoundException(
                                "Exercise " + input.libraryItemId() + " not found"));
                String notes = blankToNull(input.notes());
                boolean weekly = section.variesByWeek();
                String wk2 = weekly ? input.setsRepsWk2() : null;
                String wk3 = weekly ? input.setsRepsWk3() : null;
                String wk4 = weekly ? input.setsRepsWk4() : null;
                if (input.id() == null) {
                    exercises.add(new DayExercise(coach.getOrganization(), section, item,
                            input.setsRepsWk1(), wk2, wk3, wk4, input.cueOverride(), notes, exerciseOrder++));
                    continue;
                }
                DayExercise exercise = existingExercises.remove(input.id());
                if (exercise == null) {
                    throw new NotFoundException("Day exercise " + input.id() + " not found");
                }
                exercise.revise(section, item, input.setsRepsWk1(), wk2, wk3, wk4, input.cueOverride(), notes,
                        exerciseOrder++);
                exercises.add(exercise);
            }
        }
        dayExercises.deleteAll(List.copyOf(existingExercises.values()));
        dayExercises.saveAll(exercises);
        daySections.deleteAll(List.copyOf(existingSections.values()));

        return describeDay(day.getId());
    }

    List<DaySectionView> describeDay(UUID trainingDayId) {
        Map<UUID, List<DayExerciseView>> exercisesBySection = dayExercises.findAllByTrainingDayId(trainingDayId)
                .stream()
                .collect(Collectors.groupingBy(exercise -> exercise.getSection().getId(),
                        Collectors.mapping(DaySectionService::describe, Collectors.toList())));
        return daySections.findAllByTrainingDayId(trainingDayId).stream()
                .map(section -> new DaySectionView(section.getId(), section.getTitle(), section.getNote(),
                        section.variesByWeek(), section.getSortOrder(), exercisesBySection.getOrDefault(section.getId(), List.of())))
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
                if (day.getPlayer() == null || !day.getPlayer().getUser().getId().equals(userId)) {
                    throw new NotFoundException("Training day " + trainingDayId + " not found");
                }
            }
            case COACH -> {
                if (!editableBy(day, coach(userId))) {
                    throw new NotFoundException("Training day " + trainingDayId + " not found");
                }
            }
            case ADMIN -> {
                // An admin may read any player's training day.
            }
        }
        return day;
    }

    // A template's day is shared by every coach of the organisation; a player's day only by their coach.
    private static boolean editableBy(TrainingDay day, CoachProfile coach) {
        if (day.getTemplate() != null) {
            return day.getOrganization().getId().equals(coach.getOrganization().getId());
        }
        return day.getPlayer().getCoach() != null && day.getPlayer().getCoach().getId().equals(coach.getId());
    }

    private CoachProfile coach(UUID coachUserId) {
        return coachProfiles.findByUserId(coachUserId)
                .orElseThrow(() -> new NotFoundException("No coach profile for this account"));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
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
                exercise.getNotes(),
                exercise.getSetsRepsWk1(),
                exercise.getSetsRepsWk2(),
                exercise.getSetsRepsWk3(),
                exercise.getSetsRepsWk4(),
                exercise.getSortOrder());
    }
}
