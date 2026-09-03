package com.barbonperformance.analysis.video;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "analysis.video")
public record VideoProperties(
        @NotBlank String apiBaseUrl,
        @NotBlank String accountId,
        @NotBlank String apiToken,
        @NotBlank String webhookSecret,
        @NotNull Duration playbackTokenTtl,
        @NotNull Duration webhookTolerance,
        @Positive int maxDurationSeconds) {
}
