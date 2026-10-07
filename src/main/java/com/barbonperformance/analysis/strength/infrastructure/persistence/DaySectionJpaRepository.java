package com.barbonperformance.analysis.strength.infrastructure.persistence;

import com.barbonperformance.analysis.strength.domain.DaySection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface DaySectionJpaRepository extends JpaRepository<DaySection, UUID> {

    List<DaySection> findAllByTrainingDayIdOrderBySortOrder(UUID trainingDayId);
}
