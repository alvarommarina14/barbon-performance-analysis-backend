package com.barbonperformance.analysis.video.infrastructure.gateway;

import com.barbonperformance.analysis.video.VideoProperties;
import com.barbonperformance.analysis.video.domain.StreamGateway;
import com.barbonperformance.analysis.video.domain.StreamUnavailableException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

// tusd + MinIO stand in for Cloudflare Stream when no Stream account is available. tusd owns
// the resumable upload and drops the finished file into the MinIO bucket; playback and the
// coach's original-video download are pre-signed GETs against that same object. There is no
// transcode, so the object is served as the MP4 it was uploaded as.
@Component
@ConditionalOnProperty(prefix = "analysis.video", name = "provider", havingValue = "local")
class LocalStreamGateway implements StreamGateway {

    private static final String TUS_VERSION = "1.0.0";
    private static final Set<String> LOOPBACK = Set.of("localhost", "127.0.0.1", "::1", "[::1]");
    private static final Logger log = LoggerFactory.getLogger(LocalStreamGateway.class);

    private final VideoProperties.Local config;
    private final Duration downloadUrlTtl;
    private final URI tusPublicBase;
    private final URI s3PublicBase;
    private final RestClient tusd;
    private final S3Client s3;
    // One presigner per resolved MinIO endpoint: the host is part of the SigV4 signature, so a
    // URL signed for localhost is rejected when the phone fetches it over the LAN.
    private final ConcurrentHashMap<String, S3Presigner> presigners = new ConcurrentHashMap<>();

    LocalStreamGateway(VideoProperties properties) {
        this.config = properties.local();
        this.downloadUrlTtl = properties.playbackTokenTtl();
        this.tusPublicBase = URI.create(config.publicBaseUrl());
        this.s3PublicBase = URI.create(config.s3PublicEndpoint());
        this.tusd = RestClient.builder().baseUrl(config.tusdInternalBaseUrl()).build();
        this.s3 = S3Client.builder()
                .region(Region.US_EAST_1)
                .endpointOverride(URI.create(config.s3Endpoint()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(config.s3AccessKey(), config.s3SecretKey())))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build();
        log.info("Video provider: LOCAL — tusd at {} (advertised as {}), MinIO at {}. "
                        + "When the configured host is loopback the request's own host is used, "
                        + "so a phone on the LAN gets a reachable URL without extra configuration.",
                config.tusdInternalBaseUrl(), config.publicBaseUrl(), config.s3PublicEndpoint());
    }

    @Override
    public DirectUpload createDirectUpload(DirectUploadRequest request) {
        String host = publicAuthority(tusPublicBase);
        log.debug("Creating a tusd upload at {}/files/ ({} bytes), advertising host {}",
                config.tusdInternalBaseUrl(), request.sizeBytes(), host);
        ResponseEntity<Void> response;
        try {
            response = tusd.post()
                    .uri("/files/")
                    .header("Tus-Resumable", TUS_VERSION)
                    .header("Upload-Length", Long.toString(request.sizeBytes()))
                    .header("Upload-Metadata", uploadMetadata(request))
                    // tusd runs with -behind-proxy, so it advertises the Location the client
                    // must actually reach (the dev machine's LAN address), not its own.
                    .header("X-Forwarded-Host", host)
                    .header("X-Forwarded-Proto", tusPublicBase.getScheme())
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            throw new StreamUnavailableException("tusd upload creation at "
                    + config.tusdInternalBaseUrl() + "/files/ failed: " + e.getMessage());
        }

        String location = response.getHeaders().getFirst(HttpHeaders.LOCATION);
        if (location == null) {
            throw new StreamUnavailableException("tusd creation response was missing the Location header");
        }
        URI advertised = URI.create(location);
        String path = advertised.getRawPath() == null ? "" : advertised.getRawPath();
        String query = advertised.getRawQuery() == null ? "" : "?" + advertised.getRawQuery();
        String uploadUrl = tusPublicBase.getScheme() + "://" + host + path + query;

        DirectUpload upload = new DirectUpload(objectKey(location), uploadUrl);
        log.debug("tusd upload created: streamUid={}, uploadUrl={}",
                upload.streamUid(), upload.uploadUrl());
        return upload;
    }

    @Override
    public Playback signPlayback(String streamUid, Duration ttl) {
        String url = presignGet(streamUid, ttl, b -> b.responseContentType("video/mp4"));
        return new Playback(url, url, "", Instant.now().plus(ttl));
    }

    @Override
    public DownloadResult enableDownload(String streamUid) {
        // Nothing to render lazily: the stored object is the original the coach wants.
        String url = presignGet(streamUid, downloadUrlTtl,
                b -> b.responseContentDisposition("attachment; filename=\"original.mp4\""));
        return new DownloadResult(url, true);
    }

    @Override
    public void discardUpload(String streamUid) {
        try {
            s3.deleteObject(b -> b.bucket(config.s3Bucket()).key(streamUid));
            s3.deleteObject(b -> b.bucket(config.s3Bucket()).key(streamUid + ".info"));
        } catch (RuntimeException e) {
            log.warn("Could not discard MinIO object {}: {}", streamUid, e.getMessage());
        }
    }

    private String presignGet(String key, Duration ttl, Consumer<GetObjectRequest.Builder> extra) {
        String endpoint = s3PublicBase.getScheme() + "://" + publicAuthority(s3PublicBase);
        try {
            GetObjectPresignRequest request = GetObjectPresignRequest.builder()
                    .signatureDuration(ttl)
                    .getObjectRequest(b -> {
                        b.bucket(config.s3Bucket()).key(key);
                        extra.accept(b);
                    })
                    .build();
            return presignerFor(endpoint).presignGetObject(request).url().toString();
        } catch (RuntimeException e) {
            throw new StreamUnavailableException(
                    "could not sign a MinIO URL for " + key + " at " + endpoint + ": " + e.getMessage());
        }
    }

    private S3Presigner presignerFor(String endpoint) {
        return presigners.computeIfAbsent(endpoint, e -> S3Presigner.builder()
                .region(Region.US_EAST_1)
                .endpointOverride(URI.create(e))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(config.s3AccessKey(), config.s3SecretKey())))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build());
    }

    // The authority (host[:port]) clients should use: the configured one, unless it is loopback
    // and there is an inbound request whose own host is not — that host reaches this machine
    // from wherever the caller is (the phone on the LAN, say). The port stays as configured.
    private static String publicAuthority(URI configured) {
        String host = configured.getHost();
        if (isLoopback(host)) {
            String requestHost = currentRequestHost();
            if (requestHost != null && !isLoopback(requestHost)) {
                host = requestHost;
            }
        }
        return configured.getPort() < 0 ? host : host + ":" + configured.getPort();
    }

    private static String currentRequestHost() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            return attrs.getRequest().getServerName();
        }
        return null;
    }

    private static boolean isLoopback(String host) {
        return host == null || LOOPBACK.contains(host);
    }

    // tus Upload-Metadata: comma-separated "key base64(value)" pairs.
    private static String uploadMetadata(DirectUploadRequest request) {
        List<String> pairs = new ArrayList<>();
        pairs.add("filetype " + base64("video/mp4"));
        request.metadata().forEach((key, value) -> pairs.add(key + " " + base64(value)));
        return String.join(",", pairs);
    }

    private static String base64(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    // The s3store upload id is "<objectKey>+<multipartUploadId>"; the object in the bucket,
    // and the key the post-finish hook reports, is just the part before the "+".
    private static String objectKey(String location) {
        String lastSegment = location.substring(location.lastIndexOf('/') + 1);
        int plus = lastSegment.indexOf('+');
        return plus < 0 ? lastSegment : lastSegment.substring(0, plus);
    }
}
