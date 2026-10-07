package com.barbonperformance.analysis.strength;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "analysis.strength")
public record StrengthProperties(
        @NotBlank String publicBaseUrl,
        @NotBlank String linkSecret,
        @NotNull Duration programPdfLinkTtl) {
}
