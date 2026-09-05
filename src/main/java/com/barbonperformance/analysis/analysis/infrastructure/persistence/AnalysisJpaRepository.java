package com.barbonperformance.analysis.analysis.infrastructure.persistence;

import com.barbonperformance.analysis.analysis.domain.Analysis;
import com.barbonperformance.analysis.analysis.domain.AnalysisStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface AnalysisJpaRepository extends JpaRepository<Analysis, UUID> {

    @Query("""
            select distinct a from Analysis a
            join fetch a.coach c
            join fetch c.user
            join fetch a.player p
            join fetch p.user
            left join fetch a.submission
            left join fetch a.feedbackVideoAsset
            left join fetch a.findings
            where a.id = :id
            """)
    Optional<Analysis> findDetailedById(UUID id);

    @Query("""
            select a from Analysis a
            join fetch a.coach c
            join fetch c.user
            join fetch a.player p
            join fetch p.user
            where a.submission.id = :submissionId
            """)
    Optional<Analysis> findBySubmissionId(UUID submissionId);

    @Query("""
            select distinct a from Analysis a
            join fetch a.coach c
            join fetch c.user
            join fetch a.player p
            join fetch p.user
            left join fetch a.submission
            left join fetch a.feedbackVideoAsset
            left join fetch a.findings
            where p.id = :playerProfileId
              and a.status = :status
            order by a.publishedAt desc
            """)
    List<Analysis> findAllByPlayerIdAndStatus(UUID playerProfileId, AnalysisStatus status);

    @Query("""
            select distinct a from Analysis a
            join fetch a.coach c
            join fetch c.user
            join fetch a.player p
            join fetch p.user
            left join fetch a.submission
            left join fetch a.feedbackVideoAsset
            left join fetch a.findings
            where c.id = :coachProfileId
            order by a.createdAt desc
            """)
    List<Analysis> findAllByCoachId(UUID coachProfileId);

    @Query("""
            select distinct a from Analysis a
            join fetch a.coach c
            join fetch c.user
            join fetch a.player p
            join fetch p.user
            left join fetch a.submission
            left join fetch a.feedbackVideoAsset
            left join fetch a.findings
            where c.id = :coachProfileId
              and p.id = :playerProfileId
            order by a.createdAt desc
            """)
    List<Analysis> findAllByCoachIdAndPlayerId(UUID coachProfileId, UUID playerProfileId);
}
