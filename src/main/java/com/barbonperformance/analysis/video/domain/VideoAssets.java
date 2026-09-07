package com.barbonperformance.analysis.video.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VideoAssets {

    Optional<VideoAsset> findById(UUID id);

    Optional<VideoAsset> findByStreamUid(String streamUid);

    // Player uploads created before the cutoff that no submission ever pointed at.
    List<VideoAsset> findOrphanedUploads(Instant createdBefore);

    VideoAsset save(VideoAsset asset);

    void delete(VideoAsset asset);
}
