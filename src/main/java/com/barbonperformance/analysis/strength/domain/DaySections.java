package com.barbonperformance.analysis.strength.domain;

import java.util.List;
import java.util.UUID;

public interface DaySections {

    List<DaySection> findAllByTrainingDayId(UUID trainingDayId);

    void deleteAll(List<DaySection> sections);

    List<DaySection> saveAll(List<DaySection> sections);
}
