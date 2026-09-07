package com.barbonperformance.analysis.video.infrastructure.gateway;

import com.barbonperformance.analysis.video.VideoProperties;
import com.barbonperformance.analysis.video.domain.StreamEvent;
import com.barbonperformance.analysis.video.domain.StreamWebhooks;
import com.barbonperformance.analysis.video.domain.VideoAssetStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

// tusd calls its post-finish HTTP hook once the upload lands in MinIO. There is no transcode
// step, so "finished" is the local equivalent of Cloudflare's "ready": duration and thumbnail
// are simply unknown here. The hook is authenticated by a shared secret passed in the hook URL
// because tusd cannot attach a static header.
@Component
@ConditionalOnProperty(prefix = "analysis.video", name = "provider", havingValue = "local")
class LocalStreamWebhooks implements StreamWebhooks {

    // Its own reader rather than the application's: this parses somebody else's payload shape.
    private static final ObjectMapper JSON = new ObjectMapper();

    private final byte[] hookSecret;

    LocalStreamWebhooks(VideoProperties properties) {
        this.hookSecret = properties.local().hookSecret().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public boolean signatureIsValid(String rawBody, String signatureHeader) {
        if (signatureHeader == null) {
            return false;
        }
        return MessageDigest.isEqual(hookSecret, signatureHeader.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Optional<StreamEvent> parse(String rawBody) {
        JsonNode root;
        try {
            root = JSON.readTree(rawBody);
        } catch (Exception e) {
            return Optional.empty();
        }

        if (!"post-finish".equals(root.path("Type").asText(null))) {
            return Optional.empty();
        }

        JsonNode upload = root.path("Event").path("Upload");
        String key = upload.path("Storage").path("Key").asText(null);
        if (key == null || key.isBlank()) {
            return Optional.empty();
        }
        Long size = upload.path("Size").isNumber() ? upload.path("Size").asLong() : null;

        return Optional.of(new StreamEvent(key, VideoAssetStatus.READY, key, null, null, size));
    }
}
