package com.barbonperformance.analysis.video.infrastructure.persistence;

import com.barbonperformance.analysis.video.domain.VideoAsset;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface VideoAssetJpaRepository extends JpaRepository<VideoAsset, UUID> {

    Optional<VideoAsset> findByStreamUid(String streamUid);

    @Query("""
            select a from VideoAsset a
            where a.kind = com.barbonperformance.analysis.video.domain.VideoAssetKind.PLAYER_UPLOAD
              and a.createdAt < :createdBefore
              and not exists (select 1 from VideoSubmission s where s.videoAsset = a)
            """)
    List<VideoAsset> findOrphanedUploads(Instant createdBefore);
}
