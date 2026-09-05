package com.barbonperformance.analysis.video.domain;

import java.time.Duration;
import java.util.Map;

public interface StreamGateway {

    DirectUpload createDirectUpload(DirectUploadRequest request);

    // A short-lived token the client appends to the HLS manifest URL. Issued only after the
    // caller has been checked against the asset, because every video here is client footage.
    String signPlayback(String streamUid, Duration ttl);

    // Enables the downloadable MP4 for a Stream video so the coach can pull the original into
    // their editor. The first call kicks off the render; the URL is only usable once ready.
    DownloadResult enableDownload(String streamUid);

    record DirectUploadRequest(
            String creatorId,
            int maxDurationSeconds,
            long sizeBytes,
            Map<String, String> metadata) {
    }

    record DirectUpload(String streamUid, String uploadUrl) {
    }

    record DownloadResult(String url, boolean ready) {
    }
}
