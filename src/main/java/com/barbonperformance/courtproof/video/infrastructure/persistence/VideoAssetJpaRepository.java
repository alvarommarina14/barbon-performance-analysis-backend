package com.barbonperformance.courtproof.video.infrastructure.persistence;

import com.barbonperformance.courtproof.video.domain.VideoAsset;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface VideoAssetJpaRepository extends JpaRepository<VideoAsset, UUID> {

    Optional<VideoAsset> findByStreamUid(String streamUid);
}
