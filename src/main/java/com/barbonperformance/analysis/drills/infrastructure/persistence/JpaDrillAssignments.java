package com.barbonperformance.analysis.drills.infrastructure.persistence;

import com.barbonperformance.analysis.drills.domain.DrillAssignment;
import com.barbonperformance.analysis.drills.domain.DrillAssignments;
import com.barbonperformance.analysis.drills.domain.DrillCategory;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
class JpaDrillAssignments implements DrillAssignments {

    private final DrillAssignmentJpaRepository jpa;

    JpaDrillAssignments(DrillAssignmentJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public List<DrillAssignment> findAllByAnalysisId(UUID analysisId) {
        return jpa.findAllByAnalysisIdOrderBySortOrder(analysisId);
    }

    @Override
    public List<DrillAssignment> findAllByPlayerId(UUID playerProfileId, DrillCategory category) {
        return jpa.findAllByPlayerId(playerProfileId, category);
    }

    @Override
    public boolean existsByLibraryItemId(UUID libraryItemId) {
        return jpa.existsByLibraryItemId(libraryItemId);
    }

    @Override
    public void deleteAllByAnalysisId(UUID analysisId) {
        jpa.deleteAllByAnalysisId(analysisId);
    }

    @Override
    public List<DrillAssignment> saveAll(List<DrillAssignment> assignments) {
        return jpa.saveAll(assignments);
    }
}
