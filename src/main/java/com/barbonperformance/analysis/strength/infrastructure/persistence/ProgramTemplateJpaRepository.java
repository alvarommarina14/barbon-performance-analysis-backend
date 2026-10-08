package com.barbonperformance.analysis.strength.infrastructure.persistence;

import com.barbonperformance.analysis.strength.domain.ProgramTemplate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface ProgramTemplateJpaRepository extends JpaRepository<ProgramTemplate, UUID> {

    List<ProgramTemplate> findAllByOrganizationIdOrderByName(UUID orgId);

    boolean existsByOrganizationIdAndName(UUID orgId, String name);
}
