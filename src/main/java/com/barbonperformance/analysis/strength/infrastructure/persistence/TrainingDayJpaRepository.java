package com.barbonperformance.analysis.strength.infrastructure.persistence;

import com.barbonperformance.analysis.strength.domain.TrainingDay;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface TrainingDayJpaRepository extends JpaRepository<TrainingDay, UUID> {

    List<TrainingDay> findAllByPlayerIdOrderBySortOrder(UUID playerProfileId);
}
