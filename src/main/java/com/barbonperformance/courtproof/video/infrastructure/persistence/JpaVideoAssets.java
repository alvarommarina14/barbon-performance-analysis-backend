package com.barbonperformance.courtproof.video.infrastructure.persistence;

import com.barbonperformance.courtproof.video.domain.VideoAsset;
import com.barbonperformance.courtproof.video.domain.VideoAssets;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
class JpaVideoAssets implements VideoAssets {

    private final VideoAssetJpaRepository jpa;

    JpaVideoAssets(VideoAssetJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<VideoAsset> findById(UUID id) {
        return jpa.findById(id);
    }

    @Override
    public Optional<VideoAsset> findByStreamUid(String streamUid) {
        return jpa.findByStreamUid(streamUid);
    }

    @Override
    public VideoAsset save(VideoAsset asset) {
        return jpa.save(asset);
    }
}
