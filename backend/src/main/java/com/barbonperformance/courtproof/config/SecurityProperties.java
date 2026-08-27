package com.barbonperformance.courtproof.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * @param jwtSecret base64-encoded HMAC key, at least 256 bits. Supplied by the environment in
 *                  every deployment; the development default lives in application.yml.
 * @param accessTokenTtl short by design — a stolen access token cannot be revoked, only outlived.
 * @param refreshTokenTtl how long a session survives without the player opening the app.
 * @param organizationSlug there is one organisation today; this is how the code finds it.
 */
@Validated
@ConfigurationProperties(prefix = "courtproof.security")
public record SecurityProperties(
        @NotBlank String jwtSecret,
        @NotNull Duration accessTokenTtl,
        @NotNull Duration refreshTokenTtl,
        @NotBlank String organizationSlug) {
}
