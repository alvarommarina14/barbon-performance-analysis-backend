package com.barbonperformance.analysis.video.infrastructure.gateway;

import com.barbonperformance.analysis.video.VideoProperties;
import com.barbonperformance.analysis.video.domain.StreamGateway;
import com.barbonperformance.analysis.video.domain.StreamUnavailableException;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@ConditionalOnProperty(prefix = "analysis.video", name = "provider", havingValue = "cloudflare",
        matchIfMissing = true)
class CloudflareStreamGateway implements StreamGateway {

    private static final String TUS_VERSION = "1.0.0";

    private static final ParameterizedTypeReference<CloudflareResponse<TokenResult>> TOKEN =
            new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<CloudflareResponse<DownloadsResult>> DOWNLOADS =
            new ParameterizedTypeReference<>() {
            };

    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(CloudflareStreamGateway.class);

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
        log.info("Video provider: CLOUDFLARE — Stream API at {}, account {}",
                properties.apiBaseUrl(), properties.accountId());
    }

    // A resumable (tus) direct-creator upload: the client uploads courtside over mobile data and
    // must be able to resume. The one-time URL comes back in the Location header and needs no
    // credential; requiresignedurls keeps the footage private for the life of the video.
    @Override
    public DirectUpload createDirectUpload(DirectUploadRequest request) {
        ResponseEntity<Void> response;
        try {
            response = http.post()
                    .uri("/accounts/{account}/stream", properties.accountId())
                    .header("Tus-Resumable", TUS_VERSION)
                    .header("Upload-Length", Long.toString(request.sizeBytes()))
                    .header("Upload-Creator", request.creatorId())
                    .header("Upload-Metadata", uploadMetadata(request))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            throw new StreamUnavailableException(
                    "Cloudflare tus upload request failed: " + e.getMessage());
        }

        String uploadUrl = response.getHeaders().getFirst("Location");
        String streamUid = response.getHeaders().getFirst("stream-media-id");
        if (uploadUrl == null || streamUid == null) {
            throw new StreamUnavailableException(
                    "Cloudflare tus response was missing the Location or stream-media-id header");
        }
        return new DirectUpload(streamUid, uploadUrl);
    }

    // tus Upload-Metadata: comma-separated "key base64(value)" pairs; a valueless key means true.
    private String uploadMetadata(DirectUploadRequest request) {
        List<String> pairs = new ArrayList<>();
        pairs.add("requiresignedurls");
        pairs.add("maxDurationSeconds " + base64(Integer.toString(request.maxDurationSeconds())));
        request.metadata().forEach((key, value) -> pairs.add(key + " " + base64(value)));
        return String.join(",", pairs);
    }

    private static String base64(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Playback signPlayback(String streamUid, Duration ttl) {
        // Letting Cloudflare mint the token keeps the RSA signing key off this machine
        // entirely. The cost is one round trip per playback request.
        TokenResult result = call(
                () -> http.post()
                        .uri("/accounts/{account}/stream/{uid}/token", properties.accountId(), streamUid)
                        .body(Map.of("exp", Instant.now().plus(ttl).getEpochSecond()))
                        .retrieve()
                        .body(TOKEN),
                "playback token");

        String root = properties.deliveryBaseUrl() + "/" + result.token();
        String base = root + "/manifest/video";
        return new Playback(base + ".m3u8", base + ".mpd", root + "/watch", result.token(),
                Instant.now().plus(ttl));
    }

    @Override
    public void discardUpload(String streamUid) {
        try {
            http.delete()
                    .uri("/accounts/{account}/stream/{uid}", properties.accountId(), streamUid)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            log.warn("Could not discard Stream video {}: {}", streamUid, e.getMessage());
        }
    }

    @Override
    public DownloadResult enableDownload(String streamUid) {
        DownloadsResult result = call(
                () -> http.post()
                        .uri("/accounts/{account}/stream/{uid}/downloads", properties.accountId(), streamUid)
                        .retrieve()
                        .body(DOWNLOADS),
                "download");

        DownloadEntry entry = result.defaultEntry();
        if (entry == null) {
            throw new StreamUnavailableException("Cloudflare returned no download for " + streamUid);
        }
        return new DownloadResult(entry.url(), "ready".equals(entry.status()));
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

    record TokenResult(String token) {
    }

    record DownloadsResult(@JsonProperty("default") DownloadEntry defaultEntry) {
    }

    record DownloadEntry(String status, String url) {
    }
}
