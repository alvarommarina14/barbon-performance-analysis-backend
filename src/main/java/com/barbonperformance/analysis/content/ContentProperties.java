package com.barbonperformance.analysis.content;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

// PDFs and the recording-tips video are plain objects behind pre-signed PUT/GET URLs — MinIO
// locally, Cloudflare R2 in production — both S3-compatible, so one client configuration serves
// either. Unlike analysis.video there is no separate provider switch: nothing here needs the
// tus/webhook machinery a real streaming CDN requires.
@Validated
@ConfigurationProperties(prefix = "analysis.content")
public record ContentProperties(
        @NotBlank String s3Endpoint,
        @NotBlank String s3PublicEndpoint,
        @NotBlank String s3Bucket,
        @NotBlank String s3AccessKey,
        @NotBlank String s3SecretKey,
        @NotNull Duration presignTtl) {
}
