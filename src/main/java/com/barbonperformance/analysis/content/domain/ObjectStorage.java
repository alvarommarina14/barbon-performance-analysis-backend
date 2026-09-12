package com.barbonperformance.analysis.content.domain;

import java.time.Duration;

public interface ObjectStorage {

    PresignedUpload presignUpload(String key, Duration ttl, String contentType);

    String presignDownload(String key, Duration ttl);

    record PresignedUpload(String uploadUrl, String objectKey) {
    }
}
