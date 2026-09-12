package com.barbonperformance.analysis.drills.infrastructure.persistence;

import com.barbonperformance.analysis.drills.domain.LibraryItem;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface LibraryItemJpaRepository extends JpaRepository<LibraryItem, UUID> {

    List<LibraryItem> findAllByOrganizationId(UUID orgId);
}
