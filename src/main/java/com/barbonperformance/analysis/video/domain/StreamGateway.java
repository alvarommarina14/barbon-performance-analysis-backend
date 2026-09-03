package com.barbonperformance.analysis.video.domain;

import java.time.Duration;
import java.util.Map;

public interface StreamGateway {

    DirectUpload createDirectUpload(DirectUploadRequest request);

    // A short-lived token the client appends to the HLS manifest URL. Issued only after the
    // caller has been checked against the asset, because every video here is client footage.
    String signPlayback(String streamUid, Duration ttl);

    record DirectUploadRequest(
            String creatorId,
            int maxDurationSeconds,
            Map<String, String> metadata) {
    }

    record DirectUpload(String streamUid, String uploadUrl) {
    }
}
