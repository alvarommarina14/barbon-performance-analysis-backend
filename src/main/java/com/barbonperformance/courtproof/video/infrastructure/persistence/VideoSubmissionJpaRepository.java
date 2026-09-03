package com.barbonperformance.courtproof.video.infrastructure.persistence;

import com.barbonperformance.courtproof.video.domain.VideoSubmission;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface VideoSubmissionJpaRepository extends JpaRepository<VideoSubmission, UUID> {

    @Query("""
            select s from VideoSubmission s
            join fetch s.videoAsset
            join fetch s.player p
            join fetch p.user
            where s.id = :id
            """)
    Optional<VideoSubmission> findDetailedById(UUID id);

    // Entitlement checks need the player's coach, so this one fetches it too.
    @Query("""
            select s from VideoSubmission s
            join fetch s.videoAsset a
            join fetch s.player p
            join fetch p.user
            left join fetch p.coach
            where a.id = :videoAssetId
            """)
    Optional<VideoSubmission> findByAssetId(UUID videoAssetId);

    @Query("""
            select s from VideoSubmission s
            join fetch s.videoAsset
            join fetch s.player p
            join fetch p.user
            where p.id = :playerProfileId
            order by s.createdAt desc
            """)
    List<VideoSubmission> findAllByPlayerId(UUID playerProfileId);

    // The coach's pending queue: their own clients only, oldest first so nobody is starved,
    // and only what is actually watchable.
    @Query("""
            select s from VideoSubmission s
            join fetch s.videoAsset a
            join fetch s.player p
            join fetch p.user
            where p.coach.id = :coachProfileId
              and s.status <> com.barbonperformance.courtproof.video.domain.SubmissionStatus.ANALYZED
              and a.status = com.barbonperformance.courtproof.video.domain.VideoAssetStatus.READY
            order by s.createdAt asc
            """)
    List<VideoSubmission> findQueueForCoach(UUID coachProfileId);
}
