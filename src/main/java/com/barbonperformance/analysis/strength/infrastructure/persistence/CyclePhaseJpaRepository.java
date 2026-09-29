package com.barbonperformance.analysis.strength.infrastructure.persistence;

import com.barbonperformance.analysis.strength.domain.CyclePhase;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface CyclePhaseJpaRepository extends JpaRepository<CyclePhase, UUID> {

    List<CyclePhase> findAllByPlayerIdOrderByWeekNumber(UUID playerProfileId);

    Optional<CyclePhase> findByPlayerIdAndWeekNumber(UUID playerProfileId, int weekNumber);

    List<CyclePhase> findAllByOrganizationIdAndTemplateNameIsNotNullOrderByTemplateNameAscWeekNumberAsc(
            UUID orgId);

    List<CyclePhase> findAllByOrganizationIdAndTemplateNameOrderByWeekNumber(UUID orgId, String templateName);

    boolean existsByOrganizationIdAndTemplateName(UUID orgId, String templateName);

    void deleteAllByOrganizationIdAndTemplateName(UUID orgId, String templateName);
}
