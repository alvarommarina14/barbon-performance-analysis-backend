package com.barbonperformance.analysis.video.infrastructure.persistence;

import com.barbonperformance.analysis.video.domain.VideoAsset;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface VideoAssetJpaRepository extends JpaRepository<VideoAsset, UUID> {

    Optional<VideoAsset> findByStreamUid(String streamUid);
}
