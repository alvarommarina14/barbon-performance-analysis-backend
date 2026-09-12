package com.barbonperformance.analysis.drills.domain;

import java.util.List;
import java.util.UUID;

public interface DrillAssignments {

    List<DrillAssignment> findAllByAnalysisId(UUID analysisId);

    // category null means every category.
    List<DrillAssignment> findAllByPlayerId(UUID playerProfileId, DrillCategory category);

    boolean existsByLibraryItemId(UUID libraryItemId);

    void deleteAllByAnalysisId(UUID analysisId);

    List<DrillAssignment> saveAll(List<DrillAssignment> assignments);
}
