package com.barbonperformance.analysis.video.application;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// Runs sweepOrphanedUploads on a timer. Split from the service so the schedule is easy to see
// and to switch off (analysis.video.sweep-enabled=false) without touching the use case.
@Component
@ConditionalOnProperty(prefix = "analysis.video", name = "sweep-enabled", matchIfMissing = true)
class OrphanedUploadSweeper {

    private final VideoUploadService uploads;

    OrphanedUploadSweeper(VideoUploadService uploads) {
        this.uploads = uploads;
    }

    @Scheduled(
            initialDelayString = "${analysis.video.sweep-initial-delay:PT2M}",
            fixedDelayString = "${analysis.video.sweep-interval:PT15M}")
    void sweep() {
        uploads.sweepOrphanedUploads();
    }
}
