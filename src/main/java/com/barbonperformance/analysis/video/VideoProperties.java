package com.barbonperformance.analysis.video;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "analysis.video")
public record VideoProperties(
        @NotBlank String provider,
        String apiBaseUrl,
        String accountId,
        String apiToken,
        @NotBlank String webhookSecret,
        @NotBlank String deliveryBaseUrl,
        @NotNull Duration playbackTokenTtl,
        @NotNull Duration webhookTolerance,
        // How long an upload may sit unfinished before the submission is swept away as an orphan.
        @NotNull Duration orphanedUploadGrace,
        @Positive int maxDurationSeconds,
        @Valid Local local) {

    public VideoProperties {
        if ("cloudflare".equals(provider)
                && (isBlank(apiBaseUrl) || isBlank(accountId) || isBlank(apiToken))) {
            throw new IllegalStateException(
                    "analysis.video.provider=cloudflare needs api-base-url, account-id and api-token");
        }
        if ("local".equals(provider) && local == null) {
            throw new IllegalStateException("analysis.video.provider=local needs analysis.video.local.*");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public record Local(
            @NotBlank String tusdInternalBaseUrl,
            @NotBlank String publicBaseUrl,
            @NotBlank String s3Endpoint,
            @NotBlank String s3PublicEndpoint,
            @NotBlank String s3Bucket,
            @NotBlank String s3AccessKey,
            @NotBlank String s3SecretKey,
            @NotBlank String hookSecret) {
    }
}
