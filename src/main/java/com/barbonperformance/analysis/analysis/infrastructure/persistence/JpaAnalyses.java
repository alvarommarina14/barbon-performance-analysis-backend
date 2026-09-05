package com.barbonperformance.analysis.analysis.infrastructure.persistence;

import com.barbonperformance.analysis.analysis.domain.Analyses;
import com.barbonperformance.analysis.analysis.domain.Analysis;
import com.barbonperformance.analysis.analysis.domain.AnalysisStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
class JpaAnalyses implements Analyses {

    private final AnalysisJpaRepository jpa;

    JpaAnalyses(AnalysisJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<Analysis> findDetailedById(UUID id) {
        return jpa.findDetailedById(id);
    }

    @Override
    public Optional<Analysis> findBySubmissionId(UUID submissionId) {
        return jpa.findBySubmissionId(submissionId);
    }

    @Override
    public List<Analysis> findAllByPlayerIdAndStatus(UUID playerProfileId, AnalysisStatus status) {
        return jpa.findAllByPlayerIdAndStatus(playerProfileId, status);
    }

    @Override
    public List<Analysis> findAllByCoachId(UUID coachProfileId) {
        return jpa.findAllByCoachId(coachProfileId);
    }

    @Override
    public List<Analysis> findAllByCoachIdAndPlayerId(UUID coachProfileId, UUID playerProfileId) {
        return jpa.findAllByCoachIdAndPlayerId(coachProfileId, playerProfileId);
    }

    @Override
    public Analysis save(Analysis analysis) {
        return jpa.save(analysis);
    }

    @Override
    public void delete(Analysis analysis) {
        jpa.delete(analysis);
    }
}
