package com.barbonperformance.analysis.video.domain;

import java.util.Optional;
import java.util.UUID;

public interface VideoAssets {

    Optional<VideoAsset> findById(UUID id);

    Optional<VideoAsset> findByStreamUid(String streamUid);

    VideoAsset save(VideoAsset asset);
}
