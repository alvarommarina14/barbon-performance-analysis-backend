package com.barbonperformance.analysis.video.infrastructure.persistence;

import com.barbonperformance.analysis.video.domain.VideoSubmission;
import com.barbonperformance.analysis.video.domain.VideoSubmissions;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
class JpaVideoSubmissions implements VideoSubmissions {

    private final VideoSubmissionJpaRepository jpa;

    JpaVideoSubmissions(VideoSubmissionJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<VideoSubmission> findById(UUID id) {
        return jpa.findDetailedById(id);
    }

    @Override
    public Optional<VideoSubmission> findByAssetId(UUID videoAssetId) {
        return jpa.findByAssetId(videoAssetId);
    }

    @Override
    public List<VideoSubmission> findAllByPlayerId(UUID playerProfileId) {
        return jpa.findAllByPlayerId(playerProfileId);
    }

    @Override
    public List<VideoSubmission> findQueueForCoach(UUID coachProfileId) {
        return jpa.findQueueForCoach(coachProfileId);
    }

    @Override
    public VideoSubmission save(VideoSubmission submission) {
        return jpa.save(submission);
    }

    @Override
    public void delete(VideoSubmission submission) {
        jpa.delete(submission);
    }
}
