package com.barbonperformance.analysis.strength.application;

import com.barbonperformance.analysis.strength.domain.DayExercise;
import com.barbonperformance.analysis.strength.domain.DayExercises;
import com.barbonperformance.analysis.strength.domain.DaySection;
import com.barbonperformance.analysis.strength.domain.DaySections;
import com.barbonperformance.analysis.strength.domain.TrainingDay;
import com.barbonperformance.analysis.strength.domain.TrainingDays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.stereotype.Component;

@Component
class ProgramCopier {

    private final TrainingDays trainingDays;
    private final DaySections daySections;
    private final DayExercises dayExercises;

    ProgramCopier(TrainingDays trainingDays, DaySections daySections, DayExercises dayExercises) {
        this.trainingDays = trainingDays;
        this.daySections = daySections;
        this.dayExercises = dayExercises;
    }

    // newDay builds the copy of one day for its new owner; blocks and exercises follow it unchanged.
    void copyDays(List<TrainingDay> source, Function<TrainingDay, TrainingDay> newDay) {
        List<TrainingDay> copies = trainingDays.saveAll(source.stream().map(newDay).toList());
        for (int i = 0; i < source.size(); i++) {
            copyContent(source.get(i), copies.get(i));
        }
    }

    private void copyContent(TrainingDay from, TrainingDay to) {
        List<DaySection> fromSections = daySections.findAllByTrainingDayId(from.getId());
        List<DaySection> toSections = daySections.saveAll(fromSections.stream()
                .map(section -> new DaySection(to, section.getTitle(), section.getNote(), section.getSortOrder()))
                .toList());
        Map<UUID, DaySection> sectionCopies = new HashMap<>();
        for (int i = 0; i < fromSections.size(); i++) {
            sectionCopies.put(fromSections.get(i).getId(), toSections.get(i));
        }

        dayExercises.saveAll(dayExercises.findAllByTrainingDayId(from.getId()).stream()
                .map(exercise -> new DayExercise(to.getOrganization(), sectionCopies.get(exercise.getSection().getId()),
                        exercise.getLibraryItem(), exercise.getSetsRepsWk1(), exercise.getSetsRepsWk2(),
                        exercise.getSetsRepsWk3(), exercise.getSetsRepsWk4(), exercise.getCueOverride(),
                        exercise.getNotes(), exercise.getSortOrder()))
                .toList());
    }
}
