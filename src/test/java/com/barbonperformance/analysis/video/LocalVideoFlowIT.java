package com.barbonperformance.analysis.video;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.barbonperformance.analysis.AbstractIntegrationTest;
import com.barbonperformance.analysis.auth.application.dto.AuthDtos.LoginResponse;
import com.barbonperformance.analysis.auth.application.dto.AuthDtos.RegisterRequest;
import com.barbonperformance.analysis.video.application.dto.VideoDtos.CreateSubmissionRequest;
import com.barbonperformance.analysis.video.application.dto.VideoDtos.PlaybackResponse;
import com.barbonperformance.analysis.video.application.dto.VideoDtos.SubmissionSummary;
import com.barbonperformance.analysis.video.application.dto.VideoDtos.UploadIntentRequest;
import com.barbonperformance.analysis.video.application.dto.VideoDtos.UploadIntentResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.client.RestTestClient;

// The same guarantees as VideoFlowIT, but with analysis.video.provider=local: uploads go to a
// stubbed tusd and playback is a pre-signed MinIO URL. tusd's post-finish hook stands in for
// the Cloudflare webhook.
class LocalVideoFlowIT extends AbstractIntegrationTest {

    private static final String PASSWORD = "a-long-enough-password";
    private static final String OBJECT_KEY = "objkey123";
    private static final String HOOK_SECRET = "local-hook-secret";
    private static final long SIZE_BYTES = 12_345_678L;

    @Autowired
    private JdbcTemplate jdbc;

    @DynamicPropertySource
    static void localProvider(DynamicPropertyRegistry registry) {
        registry.add("analysis.video.provider", () -> "local");
        registry.add("analysis.video.local.tusd-internal-base-url", CLOUDFLARE::baseUrl);
        registry.add("analysis.video.local.public-base-url", () -> "http://lan-host:1080");
        registry.add("analysis.video.local.s3-endpoint", () -> "http://lan-host:9000");
        registry.add("analysis.video.local.s3-public-endpoint", () -> "http://lan-host:9000");
        registry.add("analysis.video.local.hook-secret", () -> HOOK_SECRET);
    }

    @BeforeEach
    void stubTusd() {
        CLOUDFLARE.stubFor(post(urlPathEqualTo("/files/")).willReturn(aResponse()
                .withStatus(201)
                .withHeader("Location", "http://tusd:8080/files/" + OBJECT_KEY + "+bXVsdGlwYXJ0")));
    }

    @Test
    void uploadIntentReturnsAPubliclyReachableTusUrl() {
        LoginResponse player = register("player@example.com");

        UploadIntentResponse intent = uploadIntent(player);

        assertThat(intent.uploadUrl())
                .isEqualTo("http://lan-host:1080/files/" + OBJECT_KEY + "+bXVsdGlwYXJ0");
        assertThat(assetStatus(intent.videoAssetId().toString())).isEqualTo("UPLOADING");
    }

    @Test
    void aPostFinishHookMakesTheVideoPlayableThroughAPresignedUrl() {
        LoginResponse player = register("player@example.com");
        SubmissionSummary submission = submit(player);

        PlaybackResponse playback = client.get()
                .uri("/api/v1/videos/{id}/playback", submission.videoAssetId())
                .header(HttpHeaders.AUTHORIZATION, bearer(player.accessToken()))
                .exchange()
                .expectStatus().isOk()
                .returnResult(PlaybackResponse.class)
                .getResponseBody();

        assertThat(playback.hlsUrl())
                .contains("lan-host:9000")
                .contains("barbon-performance-analysis/" + OBJECT_KEY)
                .contains("X-Amz-Signature");
        assertThat(playback.token()).isEmpty();
    }

    @Test
    void aHookWithTheWrongSecretIsRefusedAndChangesNothing() {
        LoginResponse player = register("player@example.com");
        UploadIntentResponse intent = uploadIntent(player);

        sendHook("not-the-secret").expectStatus().isForbidden();

        assertThat(assetStatus(intent.videoAssetId().toString())).isEqualTo("UPLOADING");
    }

    @Test
    void replayingTheHookLeavesTheAssetUntouched() {
        LoginResponse player = register("player@example.com");
        UploadIntentResponse intent = uploadIntent(player);

        sendHook(HOOK_SECRET).expectStatus().isOk();
        String firstUpdate = updatedAt(intent.videoAssetId().toString());

        sendHook(HOOK_SECRET).expectStatus().isOk();

        assertThat(updatedAt(intent.videoAssetId().toString())).isEqualTo(firstUpdate);
    }

    @Test
    void aDifferentPlayerCannotGetAPlaybackUrlForSomebodyElsesVideo() {
        LoginResponse owner = register("owner@example.com");
        SubmissionSummary submission = submit(owner);

        LoginResponse stranger = register("stranger@example.com");

        client.get().uri("/api/v1/videos/{id}/playback", submission.videoAssetId())
                .header(HttpHeaders.AUTHORIZATION, bearer(stranger.accessToken()))
                .exchange()
                .expectStatus().isForbidden();
    }

    private LoginResponse register(String email) {
        return client.post().uri("/api/v1/auth/register")
                .body(new RegisterRequest(email, PASSWORD, "Test Player", null))
                .exchange()
                .expectStatus().isCreated()
                .returnResult(LoginResponse.class)
                .getResponseBody();
    }

    private UploadIntentResponse uploadIntent(LoginResponse session) {
        return client.post().uri("/api/v1/submissions/upload-intent")
                .header(HttpHeaders.AUTHORIZATION, bearer(session.accessToken()))
                .body(new UploadIntentRequest(SIZE_BYTES))
                .exchange()
                .expectStatus().isCreated()
                .returnResult(UploadIntentResponse.class)
                .getResponseBody();
    }

    private SubmissionSummary submit(LoginResponse session) {
        UploadIntentResponse intent = uploadIntent(session);
        sendHook(HOOK_SECRET).expectStatus().isOk();
        return client.post().uri("/api/v1/submissions")
                .header(HttpHeaders.AUTHORIZATION, bearer(session.accessToken()))
                .body(new CreateSubmissionRequest(
                        intent.videoAssetId(), "Elbow hurts", null))
                .exchange()
                .expectStatus().isCreated()
                .returnResult(SubmissionSummary.class)
                .getResponseBody();
    }

    private RestTestClient.ResponseSpec sendHook(String token) {
        String body = "{\"Type\":\"post-finish\",\"Event\":{\"Upload\":{\"Size\":1234,"
                + "\"Storage\":{\"Key\":\"" + OBJECT_KEY + "\"}}}}";
        return client.post().uri("/api/v1/webhooks/tusd?token=" + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .exchange();
    }

    private String assetStatus(String assetId) {
        return jdbc.queryForObject(
                "SELECT status FROM video_asset WHERE id = ?::uuid", String.class, assetId);
    }

    private String updatedAt(String assetId) {
        return jdbc.queryForObject(
                "SELECT updated_at::text FROM video_asset WHERE id = ?::uuid", String.class, assetId);
    }
}
