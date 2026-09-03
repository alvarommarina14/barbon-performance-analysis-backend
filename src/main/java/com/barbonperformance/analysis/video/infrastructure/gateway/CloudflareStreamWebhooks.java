package com.barbonperformance.analysis.video.infrastructure.gateway;

import com.barbonperformance.analysis.video.VideoProperties;
import com.barbonperformance.analysis.video.domain.StreamEvent;
import com.barbonperformance.analysis.video.domain.StreamWebhooks;
import com.barbonperformance.analysis.video.domain.VideoAssetStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
class CloudflareStreamWebhooks implements StreamWebhooks {

    private static final Logger log = LoggerFactory.getLogger(CloudflareStreamWebhooks.class);
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    // Its own reader rather than the application's: Boot's mapper is configured for the HTTP
    // API, and this parses somebody else's payload shape.
    private static final ObjectMapper JSON = new ObjectMapper();

    private final VideoProperties properties;

    CloudflareStreamWebhooks(VideoProperties properties) {
        this.properties = properties;
    }

    // Cloudflare sends "Webhook-Signature: time=1700000000,sig1=<hex>" and signs the string
    // "<time>.<raw body>". The timestamp is part of the signed material and is also checked
    // against a window, so a captured delivery cannot be replayed indefinitely.
    @Override
    public boolean signatureIsValid(String rawBody, String signatureHeader) {
        if (rawBody == null || signatureHeader == null) {
            return false;
        }

        String time = null;
        String signature = null;
        for (String part : signatureHeader.split(",")) {
            String[] pair = part.trim().split("=", 2);
            if (pair.length != 2) {
                continue;
            }
            if (pair[0].equals("time")) {
                time = pair[1];
            } else if (pair[0].equals("sig1")) {
                signature = pair[1];
            }
        }
        if (time == null || signature == null) {
            return false;
        }

        long issuedAt;
        try {
            issuedAt = Long.parseLong(time);
        } catch (NumberFormatException e) {
            return false;
        }

        Duration age = Duration.between(Instant.ofEpochSecond(issuedAt), Instant.now()).abs();
        if (age.compareTo(properties.webhookTolerance()) > 0) {
            log.warn("Rejecting a Stream webhook signed {} ago", age);
            return false;
        }

        byte[] expected = hmac(time + "." + rawBody);
        byte[] presented;
        try {
            presented = HexFormat.of().parseHex(signature);
        } catch (IllegalArgumentException e) {
            return false;
        }
        return MessageDigest.isEqual(expected, presented);
    }

    @Override
    public Optional<StreamEvent> parse(String rawBody) {
        JsonNode root;
        try {
            root = JSON.readTree(rawBody);
        } catch (Exception e) {
            return Optional.empty();
        }

        String uid = root.path("uid").asText(null);
        String state = root.path("status").path("state").asText(null);
        if (uid == null || state == null) {
            return Optional.empty();
        }

        VideoAssetStatus status = switch (state) {
            case "ready" -> VideoAssetStatus.READY;
            case "inprogress", "queued", "downloading" -> VideoAssetStatus.PROCESSING;
            case "error" -> VideoAssetStatus.ERRORED;
            default -> null;
        };
        if (status == null) {
            return Optional.empty();
        }

        // In Stream the playback id and the uid are the same value; the domain keeps them as
        // separate fields so a future provider that separates them does not force a migration.
        String playbackId = uid;
        Integer duration = root.path("duration").isNumber()
                ? (int) Math.round(root.path("duration").asDouble())
                : null;
        String thumbnail = root.path("thumbnail").asText(null);
        Long size = root.path("size").isNumber() ? root.path("size").asLong() : null;

        return Optional.of(new StreamEvent(uid, status, playbackId, duration, thumbnail, size));
    }

    private byte[] hmac(String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(
                    properties.webhookSecret().getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("HMAC-SHA256 is required by the JDK", e);
        }
    }
}
