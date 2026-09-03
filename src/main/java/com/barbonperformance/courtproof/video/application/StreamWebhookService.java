package com.barbonperformance.courtproof.video.application;

import com.barbonperformance.courtproof.video.domain.InvalidWebhookSignatureException;
import com.barbonperformance.courtproof.video.domain.StreamEvent;
import com.barbonperformance.courtproof.video.domain.StreamWebhooks;
import com.barbonperformance.courtproof.video.domain.VideoAsset;
import com.barbonperformance.courtproof.video.domain.VideoAssets;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StreamWebhookService {

    private static final Logger log = LoggerFactory.getLogger(StreamWebhookService.class);

    private final StreamWebhooks webhooks;
    private final VideoAssets videoAssets;

    public StreamWebhookService(StreamWebhooks webhooks, VideoAssets videoAssets) {
        this.webhooks = webhooks;
        this.videoAssets = videoAssets;
    }

    // Cloudflare retries, so this has to be safe to run twice on the same delivery. The entity
    // reports whether the transition changed anything; a repeat is logged and ignored rather
    // than being allowed to overwrite a later state.
    @Transactional
    public void handle(String rawBody, String signatureHeader) {
        if (!webhooks.signatureIsValid(rawBody, signatureHeader)) {
            throw new InvalidWebhookSignatureException("Stream webhook signature did not verify");
        }

        Optional<StreamEvent> parsed = webhooks.parse(rawBody);
        if (parsed.isEmpty()) {
            return;
        }
        StreamEvent event = parsed.get();

        Optional<VideoAsset> found = videoAssets.findByStreamUid(event.streamUid());
        if (found.isEmpty()) {
            log.warn("Stream webhook for unknown video {}", event.streamUid());
            return;
        }
        VideoAsset asset = found.get();

        boolean changed = switch (event.status()) {
            case READY -> asset.markReady(
                    event.playbackId(), event.durationSeconds(), event.thumbnailUrl(), event.sizeBytes());
            case PROCESSING -> asset.markProcessing();
            case ERRORED -> asset.markErrored();
            default -> false;
        };

        if (!changed) {
            log.debug("Duplicate Stream webhook for video {} in state {}",
                    event.streamUid(), asset.getStatus());
        }
    }
}
