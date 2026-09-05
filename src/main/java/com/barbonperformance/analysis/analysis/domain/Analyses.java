package com.barbonperformance.analysis.analysis.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface Analyses {

    Optional<Analysis> findDetailedById(UUID id);

    Optional<Analysis> findBySubmissionId(UUID submissionId);

    List<Analysis> findAllByPlayerIdAndStatus(UUID playerProfileId, AnalysisStatus status);

    List<Analysis> findAllByCoachId(UUID coachProfileId);

    List<Analysis> findAllByCoachIdAndPlayerId(UUID coachProfileId, UUID playerProfileId);

    Analysis save(Analysis analysis);

    void delete(Analysis analysis);
}
