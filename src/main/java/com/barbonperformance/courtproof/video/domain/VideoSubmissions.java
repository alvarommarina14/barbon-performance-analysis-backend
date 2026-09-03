package com.barbonperformance.courtproof.video.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VideoSubmissions {

    Optional<VideoSubmission> findById(UUID id);

    Optional<VideoSubmission> findByAssetId(UUID videoAssetId);

    List<VideoSubmission> findAllByPlayerId(UUID playerProfileId);

    List<VideoSubmission> findQueueForCoach(UUID coachProfileId);

    VideoSubmission save(VideoSubmission submission);
}
