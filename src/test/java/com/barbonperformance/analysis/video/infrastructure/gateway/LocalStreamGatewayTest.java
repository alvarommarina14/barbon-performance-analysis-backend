package com.barbonperformance.analysis.video.infrastructure.gateway;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.barbonperformance.analysis.video.VideoProperties;
import com.barbonperformance.analysis.video.domain.StreamGateway.DirectUpload;
import com.barbonperformance.analysis.video.domain.StreamGateway.DirectUploadRequest;
import com.barbonperformance.analysis.video.domain.StreamGateway.DownloadResult;
import com.barbonperformance.analysis.video.domain.StreamGateway.Playback;
import com.github.tomakehurst.wiremock.WireMockServer;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LocalStreamGatewayTest {

    private static final String PUBLIC_HOST = "192.168.1.50";

    private static WireMockServer tusd;

    private LocalStreamGateway gateway;

    @BeforeAll
    static void startTusd() {
        tusd = new WireMockServer(0);
        tusd.start();
    }

    @AfterAll
    static void stopTusd() {
        tusd.stop();
    }

    @BeforeEach
    void newGateway() {
        tusd.resetAll();
        gateway = new LocalStreamGateway(properties(tusd.baseUrl()));
    }

    @Test
    void createDirectUploadReturnsTheBareObjectKeyAndAPubliclyReachableUploadUrl() {
        tusd.stubFor(post(urlPathEqualTo("/files/")).willReturn(aResponse()
                .withStatus(201)
                .withHeader("Location", "http://tusd:8080/files/objkey123+bXVsdGlwYXJ0")));

        DirectUpload upload = gateway.createDirectUpload(new DirectUploadRequest(
                "player-1", 600, 12_345L, Map.of("videoAssetId", "asset-1")));

        assertThat(upload.streamUid()).isEqualTo("objkey123");
        assertThat(upload.uploadUrl())
                .isEqualTo("http://" + PUBLIC_HOST + ":1080/files/objkey123+bXVsdGlwYXJ0");

        tusd.verify(postRequestedFor(urlPathEqualTo("/files/"))
                .withHeader("Tus-Resumable", equalTo("1.0.0"))
                .withHeader("Upload-Length", equalTo("12345"))
                .withHeader("X-Forwarded-Host", equalTo(PUBLIC_HOST + ":1080"))
                .withHeader("X-Forwarded-Proto", equalTo("http"))
                .withHeader("Upload-Metadata", containing("videoAssetId")));
    }

    @Test
    void signPlaybackReturnsAPresignedMinioUrlForTheObject() {
        Playback playback = gateway.signPlayback("objkey123", Duration.ofHours(2));

        assertThat(playback.hlsUrl())
                .contains(PUBLIC_HOST + ":9000")
                .contains("barbon-performance-analysis/objkey123")
                .contains("X-Amz-Signature")
                .contains("X-Amz-Expires=7200");
        assertThat(playback.dashUrl()).isEqualTo(playback.hlsUrl());
        assertThat(playback.token()).isEmpty();
        assertThat(playback.expiresAt()).isAfter(Instant.now());
    }

    @Test
    void enableDownloadPresignsAnAttachmentAndIsImmediatelyReady() {
        DownloadResult download = gateway.enableDownload("objkey123");

        assertThat(download.ready()).isTrue();
        assertThat(download.url())
                .contains("barbon-performance-analysis/objkey123")
                .contains("response-content-disposition")
                .contains("X-Amz-Signature");
    }

    private static VideoProperties properties(String tusdBaseUrl) {
        VideoProperties.Local local = new VideoProperties.Local(
                tusdBaseUrl,
                "http://" + PUBLIC_HOST + ":1080",
                "http://minio:9000",
                "http://" + PUBLIC_HOST + ":9000",
                "barbon-performance-analysis",
                "bpa",
                "bpa-development",
                "local-hook-secret");
        return new VideoProperties(
                "local", null, null, null, "webhook-secret", "https://videodelivery.net",
                Duration.ofHours(2), Duration.ofMinutes(5), Duration.ofMinutes(45), 600, local);
    }
}
