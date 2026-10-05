package com.barbonperformance.analysis.content.infrastructure.persistence;

import com.barbonperformance.analysis.content.domain.DailyRoutine;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface DailyRoutineJpaRepository extends JpaRepository<DailyRoutine, UUID> {

    Optional<DailyRoutine> findByOrganizationId(UUID orgId);
}
