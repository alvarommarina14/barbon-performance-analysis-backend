package com.barbonperformance.analysis.content.infrastructure.persistence;

import com.barbonperformance.analysis.content.domain.RpeGuide;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface RpeGuideJpaRepository extends JpaRepository<RpeGuide, UUID> {

    Optional<RpeGuide> findByOrganizationId(UUID orgId);
}
