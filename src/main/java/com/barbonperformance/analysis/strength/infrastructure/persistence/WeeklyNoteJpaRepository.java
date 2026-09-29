package com.barbonperformance.analysis.strength.infrastructure.persistence;

import com.barbonperformance.analysis.strength.domain.WeeklyNote;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface WeeklyNoteJpaRepository extends JpaRepository<WeeklyNote, UUID> {

    List<WeeklyNote> findAllByPlayerIdOrderByPlanWeek(UUID playerProfileId);

    Optional<WeeklyNote> findByPlayerIdAndPlanWeek(UUID playerProfileId, int planWeek);
}
