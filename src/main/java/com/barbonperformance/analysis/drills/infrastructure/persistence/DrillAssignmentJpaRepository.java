package com.barbonperformance.analysis.drills.infrastructure.persistence;

import com.barbonperformance.analysis.drills.domain.DrillAssignment;
import com.barbonperformance.analysis.drills.domain.DrillCategory;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface DrillAssignmentJpaRepository extends JpaRepository<DrillAssignment, UUID> {

    List<DrillAssignment> findAllByAnalysisIdOrderBySortOrder(UUID analysisId);

    @Query("""
            select d from DrillAssignment d
            where d.player.id = :playerProfileId
              and (:category is null or d.libraryItem.category = :category)
            order by d.sortOrder
            """)
    List<DrillAssignment> findAllByPlayerId(UUID playerProfileId, DrillCategory category);

    boolean existsByLibraryItemId(UUID libraryItemId);

    void deleteAllByAnalysisId(UUID analysisId);
}
