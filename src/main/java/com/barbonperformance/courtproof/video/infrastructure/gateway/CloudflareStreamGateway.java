package com.barbonperformance.courtproof.video.infrastructure.gateway;

import com.barbonperformance.courtproof.video.VideoProperties;
import com.barbonperformance.courtproof.video.domain.StreamGateway;
import com.barbonperformance.courtproof.video.domain.StreamUnavailableException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
class CloudflareStreamGateway implements StreamGateway {

    private static final ParameterizedTypeReference<CloudflareResponse<DirectUploadResult>> DIRECT_UPLOAD =
            new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<CloudflareResponse<TokenResult>> TOKEN =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient http;
    private final VideoProperties properties;

    // Built here rather than injected: Boot 4 does not auto-configure a RestClient.Builder,
    // and this client wants its own base URL and credential anyway.
    CloudflareStreamGateway(VideoProperties properties) {
        this.properties = properties;
        this.http = RestClient.builder()
                .baseUrl(properties.apiBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.apiToken())
                .build();
    }

    @Override
    public DirectUpload createDirectUpload(DirectUploadRequest request) {
        // requireSignedURLs is not optional: every video here is client footage, and an
        // unsigned playback id is a public link to it for as long as the video exists.
        Map<String, Object> body = Map.of(
                "maxDurationSeconds", request.maxDurationSeconds(),
                "requireSignedURLs", true,
                "creator", request.creatorId(),
                "meta", request.metadata());

        DirectUploadResult result = call(
                () -> http.post()
                        .uri("/accounts/{account}/stream/direct_upload", properties.accountId())
                        .body(body)
                        .retrieve()
                        .body(DIRECT_UPLOAD),
                "direct upload");

        return new DirectUpload(result.uid(), result.uploadURL());
    }

    @Override
    public String signPlayback(String streamUid, Duration ttl) {
        // Letting Cloudflare mint the token keeps the RSA signing key off this machine
        // entirely. The cost is one round trip per playback request.
        TokenResult result = call(
                () -> http.post()
                        .uri("/accounts/{account}/stream/{uid}/token", properties.accountId(), streamUid)
                        .body(Map.of("exp", Instant.now().plus(ttl).getEpochSecond()))
                        .retrieve()
                        .body(TOKEN),
                "playback token");

        return result.token();
    }

    private <T> T call(java.util.function.Supplier<CloudflareResponse<T>> request, String what) {
        CloudflareResponse<T> response;
        try {
            response = request.get();
        } catch (RestClientException e) {
            throw new StreamUnavailableException("Cloudflare " + what + " request failed: " + e.getMessage());
        }
        if (response == null || !response.success() || response.result() == null) {
            String detail = response == null || response.errors() == null
                    ? "no response"
                    : response.errors().toString();
            throw new StreamUnavailableException("Cloudflare rejected the " + what + " request: " + detail);
        }
        return response.result();
    }

    record CloudflareResponse<T>(boolean success, T result, List<Map<String, Object>> errors) {
    }

    record DirectUploadResult(String uid, String uploadURL) {
    }

    record TokenResult(String token) {
    }
}
