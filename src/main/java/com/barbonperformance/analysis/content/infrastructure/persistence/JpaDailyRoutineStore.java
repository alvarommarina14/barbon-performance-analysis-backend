package com.barbonperformance.analysis.content.infrastructure.persistence;

import com.barbonperformance.analysis.content.domain.DailyRoutine;
import com.barbonperformance.analysis.content.domain.DailyRoutineStore;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
class JpaDailyRoutineStore implements DailyRoutineStore {

    private final DailyRoutineJpaRepository jpa;

    JpaDailyRoutineStore(DailyRoutineJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<DailyRoutine> findByOrgId(UUID orgId) {
        return jpa.findByOrganizationId(orgId);
    }

    @Override
    public DailyRoutine save(DailyRoutine routine) {
        return jpa.save(routine);
    }
}
