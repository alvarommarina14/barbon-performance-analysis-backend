package com.barbonperformance.analysis.content.infrastructure.gateway;

import com.barbonperformance.analysis.content.ContentProperties;
import com.barbonperformance.analysis.content.domain.ObjectStorage;
import java.net.URI;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

// MinIO locally, Cloudflare R2 in production — both S3-compatible, so a single client works for
// either; only analysis.content.* differs by environment. No delete: every key this module hands
// out is deterministic (one recording-tips PDF per org), so a re-upload just overwrites it.
//
// Same LAN workaround as LocalStreamGateway: a configured loopback host (the usual dev default)
// is swapped for the inbound request's own host, so a URL signed on the backend's laptop is still
// reachable from a phone on the LAN. The host is part of the SigV4 signature, so this means
// signing against whichever endpoint will actually be dereferenced, not rewriting the URL after.
@Component
class S3ObjectStorage implements ObjectStorage {

    private static final Set<String> LOOPBACK = Set.of("localhost", "127.0.0.1", "::1", "[::1]");

    private final ContentProperties config;
    private final URI publicBase;
    private final ConcurrentHashMap<String, S3Presigner> presigners = new ConcurrentHashMap<>();

    S3ObjectStorage(ContentProperties config) {
        this.config = config;
        this.publicBase = URI.create(config.s3PublicEndpoint());
    }

    @Override
    public PresignedUpload presignUpload(String key, Duration ttl, String contentType) {
        PutObjectPresignRequest request = PutObjectPresignRequest.builder()
                .signatureDuration(ttl)
                .putObjectRequest(b -> b.bucket(config.s3Bucket()).key(key).contentType(contentType))
                .build();
        String url = presignerFor(publicEndpoint()).presignPutObject(request).url().toString();
        return new PresignedUpload(url, key);
    }

    @Override
    public String presignDownload(String key, Duration ttl) {
        GetObjectPresignRequest request = GetObjectPresignRequest.builder()
                .signatureDuration(ttl)
                .getObjectRequest(b -> b.bucket(config.s3Bucket()).key(key))
                .build();
        return presignerFor(publicEndpoint()).presignGetObject(request).url().toString();
    }

    private String publicEndpoint() {
        return publicBase.getScheme() + "://" + publicAuthority();
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
    // and there is an inbound request whose own host is not — that host reaches this machine from
    // wherever the caller is (the phone on the LAN, say). The port stays as configured.
    private String publicAuthority() {
        String host = publicBase.getHost();
        if (isLoopback(host)) {
            String requestHost = currentRequestHost();
            if (requestHost != null && !isLoopback(requestHost)) {
                host = requestHost;
            }
        }
        return publicBase.getPort() < 0 ? host : host + ":" + publicBase.getPort();
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
}
