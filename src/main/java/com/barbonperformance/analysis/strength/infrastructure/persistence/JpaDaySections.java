package com.barbonperformance.analysis.strength.infrastructure.persistence;

import com.barbonperformance.analysis.strength.domain.DaySection;
import com.barbonperformance.analysis.strength.domain.DaySections;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
class JpaDaySections implements DaySections {

    private final DaySectionJpaRepository jpa;

    JpaDaySections(DaySectionJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public List<DaySection> findAllByTrainingDayId(UUID trainingDayId) {
        return jpa.findAllByTrainingDayIdOrderBySortOrder(trainingDayId);
    }

    @Override
    public void deleteAll(List<DaySection> sections) {
        jpa.deleteAll(sections);
    }

    @Override
    public List<DaySection> saveAll(List<DaySection> sections) {
        return jpa.saveAll(sections);
    }
}
