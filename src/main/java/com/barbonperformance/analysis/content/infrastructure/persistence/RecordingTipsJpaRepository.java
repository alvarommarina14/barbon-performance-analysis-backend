package com.barbonperformance.analysis.content.infrastructure.persistence;

import com.barbonperformance.analysis.content.domain.RecordingTips;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface RecordingTipsJpaRepository extends JpaRepository<RecordingTips, UUID> {

    Optional<RecordingTips> findByOrganizationId(UUID orgId);
}
