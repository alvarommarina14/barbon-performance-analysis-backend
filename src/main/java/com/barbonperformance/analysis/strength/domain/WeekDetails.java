package com.barbonperformance.analysis.strength.domain;

// What the coach writes for one cycle week; a row of the PDF's RPE table. effort names the RPE
// ("Light/Moderate"), repsLeft says it in reps ("You could do 3 to 4 more reps").
public record WeekDetails(
        String phaseName,
        String goal,
        String effort,
        String rpeRange,
        String repsLeft,
        String example) {
}
