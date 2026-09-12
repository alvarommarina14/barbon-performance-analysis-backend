package com.barbonperformance.analysis.content.infrastructure.gateway;

import com.barbonperformance.analysis.content.ContentProperties;
import com.barbonperformance.analysis.content.domain.ObjectStorage;
import java.net.URI;
import java.time.Duration;
import org.springframework.stereotype.Component;
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
@Component
class S3ObjectStorage implements ObjectStorage {

    private final ContentProperties config;
    private final S3Presigner presigner;

    S3ObjectStorage(ContentProperties config) {
        this.config = config;
        this.presigner = S3Presigner.builder()
                .region(Region.US_EAST_1)
                .endpointOverride(URI.create(config.s3PublicEndpoint()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(config.s3AccessKey(), config.s3SecretKey())))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build();
    }

    @Override
    public PresignedUpload presignUpload(String key, Duration ttl, String contentType) {
        PutObjectPresignRequest request = PutObjectPresignRequest.builder()
                .signatureDuration(ttl)
                .putObjectRequest(b -> b.bucket(config.s3Bucket()).key(key).contentType(contentType))
                .build();
        String url = presigner.presignPutObject(request).url().toString();
        return new PresignedUpload(url, key);
    }

    @Override
    public String presignDownload(String key, Duration ttl) {
        GetObjectPresignRequest request = GetObjectPresignRequest.builder()
                .signatureDuration(ttl)
                .getObjectRequest(b -> b.bucket(config.s3Bucket()).key(key))
                .build();
        return presigner.presignGetObject(request).url().toString();
    }
}
