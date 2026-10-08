package com.barbonperformance.analysis.strength.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public record ProgramDocument(
        String athleteName,
        String athleteLevel,
        String coachName,
        String organizationName,
        String contactEmail,
        LocalDate issuedOn,
        String about,
        String schedule,
        String cycleNote,
        List<RedFlagEntry> redFlags,
        List<Week> weeks,
        List<Day> days) {

    public String weekHeading(int number) {
        return weeks.stream()
                .filter(week -> week.number() == number)
                .findFirst()
                .map(week -> "W" + number + " " + week.phaseName())
                .orElse("Week " + number);
    }

    // The Example column only shows when the coach wrote one for some week.
    public boolean hasRpeExamples() {
        return weeks.stream().anyMatch(week -> week.example() != null && !week.example().isBlank());
    }

    public record RedFlagEntry(String title, String body) {
    }

    public record Week(int number, String phaseName, String goal, String rpeRange, String example) {
    }

    public record Day(
            String label,
            String title,
            String focus,
            Integer durationMinutes,
            TrainingDayKind kind,
            String protocol,
            List<Block> blocks) {

        public boolean isCardio() {
            return kind == TrainingDayKind.CARDIO;
        }
    }

    public record Block(String title, String note, List<Exercise> exercises) {

        // A block whose exercises do the same work every week prints one "Sets × Reps" column
        // instead of four identical week columns, the way the coach's warm-up blocks read.
        public boolean variesByWeek() {
            return exercises.stream().anyMatch(Exercise::variesByWeek);
        }
    }

    public record Exercise(String title, String cue, String notes, List<String> setsRepsByWeek, String videoUrl) {

        public boolean variesByWeek() {
            return setsRepsByWeek.stream().filter(Objects::nonNull).distinct().count() > 1;
        }

        public String setsReps() {
            return setsRepsByWeek.stream().filter(Objects::nonNull).findFirst().orElse(null);
        }
    }
}
