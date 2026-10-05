package com.barbonperformance.analysis.strength.infrastructure.persistence;

import com.barbonperformance.analysis.strength.domain.WeeklyNote;
import com.barbonperformance.analysis.strength.domain.WeeklyNotes;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
class JpaWeeklyNotes implements WeeklyNotes {

    private final WeeklyNoteJpaRepository jpa;

    JpaWeeklyNotes(WeeklyNoteJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public List<WeeklyNote> findAllByPlayerId(UUID playerProfileId) {
        return jpa.findAllByPlayerIdOrderByPlanWeek(playerProfileId);
    }

    @Override
    public Optional<WeeklyNote> findByPlayerIdAndPlanWeek(UUID playerProfileId, int planWeek) {
        return jpa.findByPlayerIdAndPlanWeek(playerProfileId, planWeek);
    }

    @Override
    public WeeklyNote save(WeeklyNote note) {
        return jpa.save(note);
    }

    @Override
    public void delete(WeeklyNote note) {
        jpa.delete(note);
    }
}
