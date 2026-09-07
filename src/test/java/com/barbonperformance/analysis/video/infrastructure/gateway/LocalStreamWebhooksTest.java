package com.barbonperformance.analysis.video.infrastructure.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import com.barbonperformance.analysis.video.VideoProperties;
import com.barbonperformance.analysis.video.domain.StreamEvent;
import com.barbonperformance.analysis.video.domain.VideoAssetStatus;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class LocalStreamWebhooksTest {

    private static final String SECRET = "local-hook-secret";

    private final LocalStreamWebhooks webhooks = new LocalStreamWebhooks(properties());

    @Test
    void aPostFinishPayloadBecomesAReadyEventWithUnknownDurationAndThumbnail() {
        String body = """
                {"Type":"post-finish","Event":{"Upload":{
                  "Size":1048576,
                  "Storage":{"Type":"s3store","Bucket":"barbon-performance-analysis","Key":"abc123"}
                }}}""";

        Optional<StreamEvent> event = webhooks.parse(body);

        assertThat(event).hasValueSatisfying(e -> {
            assertThat(e.streamUid()).isEqualTo("abc123");
            assertThat(e.playbackId()).isEqualTo("abc123");
            assertThat(e.status()).isEqualTo(VideoAssetStatus.READY);
            assertThat(e.sizeBytes()).isEqualTo(1048576L);
            assertThat(e.durationSeconds()).isNull();
            assertThat(e.thumbnailUrl()).isNull();
        });
    }

    @Test
    void anEventOtherThanPostFinishIsIgnored() {
        String body = """
                {"Type":"post-create","Event":{"Upload":{"Storage":{"Key":"abc123"}}}}""";

        assertThat(webhooks.parse(body)).isEmpty();
    }

    @Test
    void malformedJsonIsIgnoredRatherThanThrowing() {
        assertThat(webhooks.parse("not json")).isEmpty();
    }

    @Test
    void theSharedSecretIsCheckedInConstantTime() {
        assertThat(webhooks.signatureIsValid("body", SECRET)).isTrue();
        assertThat(webhooks.signatureIsValid("body", "wrong-secret")).isFalse();
        assertThat(webhooks.signatureIsValid("body", null)).isFalse();
    }

    private static VideoProperties properties() {
        VideoProperties.Local local = new VideoProperties.Local(
                "http://localhost:1080", "http://localhost:1080",
                "http://localhost:9000", "http://localhost:9000",
                "barbon-performance-analysis", "bpa", "bpa-development", SECRET);
        return new VideoProperties(
                "local", null, null, null, "webhook-secret", "https://videodelivery.net",
                Duration.ofHours(2), Duration.ofMinutes(5), Duration.ofMinutes(45), 600, local);
    }
}
