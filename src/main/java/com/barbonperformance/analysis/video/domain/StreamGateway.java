package com.barbonperformance.analysis.video.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

public interface StreamGateway {

    DirectUpload createDirectUpload(DirectUploadRequest request);

    // Short-lived, entitlement-gated playback links for one asset. The gateway owns the URL
    // shape because it differs by provider: Cloudflare returns a token spliced into a
    // manifest URL, a local store returns a pre-signed object URL with no separate token.
    Playback signPlayback(String streamUid, Duration ttl);

    // Enables the downloadable MP4 for a Stream video so the coach can pull the original into
    // their editor. The first call kicks off the render; the URL is only usable once ready.
    DownloadResult enableDownload(String streamUid);

    // Best effort: drop a video whose upload was never turned into a submission. A failure here
    // is logged, not raised — the sweep still removes the row.
    void discardUpload(String streamUid);

    record DirectUploadRequest(
            String creatorId,
            int maxDurationSeconds,
            long sizeBytes,
            Map<String, String> metadata) {
    }

    record DirectUpload(String streamUid, String uploadUrl) {
    }

    // watchUrl opens in any browser on its own, for a link that leaves the app (the program PDF).
    record Playback(String hlsUrl, String dashUrl, String watchUrl, String token, Instant expiresAt) {
    }

    record DownloadResult(String url, boolean ready) {
    }
}
