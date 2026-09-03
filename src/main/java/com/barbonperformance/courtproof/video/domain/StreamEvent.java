package com.barbonperformance.courtproof.video.domain;

public record StreamEvent(
        String streamUid,
        VideoAssetStatus status,
        String playbackId,
        Integer durationSeconds,
        String thumbnailUrl,
        Long sizeBytes) {
}
