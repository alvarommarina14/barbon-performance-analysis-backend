package com.barbonperformance.analysis.video;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.assertj.core.api.Assertions.assertThat;

import com.barbonperformance.analysis.AbstractIntegrationTest;
import com.barbonperformance.analysis.auth.application.dto.AuthDtos.LoginResponse;
import com.barbonperformance.analysis.auth.application.dto.AuthDtos.RegisterRequest;
import com.barbonperformance.analysis.video.application.VideoUploadService;
import com.barbonperformance.analysis.video.application.dto.VideoDtos.CreateSubmissionRequest;
import com.barbonperformance.analysis.video.application.dto.VideoDtos.PlaybackResponse;
import com.barbonperformance.analysis.video.application.dto.VideoDtos.SubmissionSummary;
import com.barbonperformance.analysis.video.application.dto.VideoDtos.UploadIntentRequest;
import com.barbonperformance.analysis.video.application.dto.VideoDtos.UploadIntentResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.client.RestTestClient;

class VideoFlowIT extends AbstractIntegrationTest {

    private static final String PASSWORD = "a-long-enough-password";
    private static final String STREAM_UID = "stream-uid-1";
    private static final String WEBHOOK_SECRET = "test-webhook-secret";
    private static final long SIZE_BYTES = 12_345_678L;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private VideoUploadService uploads;

    @BeforeEach
    void stubCloudflare() {
        CLOUDFLARE.stubFor(post(urlPathEqualTo("/accounts/test-account/stream"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Location", "https://upload.videodelivery.net/tus/" + STREAM_UID)
                        .withHeader("stream-media-id", STREAM_UID)));

        CLOUDFLARE.stubFor(post(urlPathMatching("/accounts/test-account/stream/.+/token"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"success":true,"errors":[],"result":{"token":"a-signed-token"}}
                                """)));
    }

    @Test
    void uploadIntentRegistersAnAssetAndReturnsTheOneTimeUploadUrl() {
        LoginResponse player = register("player@example.com");

        UploadIntentResponse intent = uploadIntent(player);

        assertThat(intent.videoAssetId()).isNotNull();
        assertThat(intent.uploadUrl()).isEqualTo("https://upload.videodelivery.net/tus/" + STREAM_UID);
        assertThat(intent.maxDurationSeconds()).isEqualTo(600);

        assertThat(assetStatus(intent.videoAssetId().toString())).isEqualTo("UPLOADING");
        assertThat(jdbc.queryForObject(
                "SELECT uploaded_by_player_id FROM video_asset WHERE id = ?::uuid", String.class,
                intent.videoAssetId().toString())).isNotNull();
        // No submission until the upload lands.
        assertThat(jdbc.queryForObject("SELECT count(*) FROM video_submission", Integer.class))
                .isZero();
    }

    // A resumable tus creation: the length is fixed up front, requiresignedurls keeps the footage
    // private, and the asset id rides along so the webhook can be matched back.
    @Test
    void uploadIntentAsksCloudflareForAPrivateResumableUpload() {
        LoginResponse player = register("player@example.com");

        uploadIntent(player);

        CLOUDFLARE.verify(postRequestedFor(urlPathEqualTo("/accounts/test-account/stream"))
                .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Bearer test-token"))
                .withHeader("Tus-Resumable", equalTo("1.0.0"))
                .withHeader("Upload-Length", equalTo(Long.toString(SIZE_BYTES)))
                .withHeader("Upload-Metadata", containing("requiresignedurls"))
                .withHeader("Upload-Metadata", containing("videoAssetId")));
    }

    @Test
    void aReadyWebhookMakesTheVideoPlayable() {
        LoginResponse player = register("player@example.com");
        UploadIntentResponse intent = uploadIntent(player);

        sendWebhook(readyPayload(), Instant.now()).expectStatus().isNoContent();

        assertThat(assetStatus(intent.videoAssetId().toString())).isEqualTo("READY");
        assertThat(jdbc.queryForObject(
                "SELECT duration_seconds FROM video_asset WHERE id = ?::uuid", Integer.class,
                intent.videoAssetId().toString())).isEqualTo(42);
        assertThat(jdbc.queryForObject(
                "SELECT thumbnail_url FROM video_asset WHERE id = ?::uuid", String.class,
                intent.videoAssetId().toString())).contains("thumbnail");
    }

    @Test
    void aWebhookWithAWrongSignatureIsRefusedAndChangesNothing() {
        LoginResponse player = register("player@example.com");
        UploadIntentResponse intent = uploadIntent(player);

        client.post().uri("/api/v1/webhooks/cloudflare-stream")
                .header("Webhook-Signature",
                        "time=" + Instant.now().getEpochSecond() + ",sig1=deadbeef")
                .contentType(MediaType.APPLICATION_JSON)
                .body(readyPayload())
                .exchange()
                .expectStatus().isForbidden();

        assertThat(assetStatus(intent.videoAssetId().toString())).isEqualTo("UPLOADING");
    }

    @Test
    void aWebhookWithNoSignatureAtAllIsRefused() {
        LoginResponse player = register("player@example.com");
        uploadIntent(player);

        client.post().uri("/api/v1/webhooks/cloudflare-stream")
                .contentType(MediaType.APPLICATION_JSON)
                .body(readyPayload())
                .exchange()
                .expectStatus().isForbidden();
    }

    // Cloudflare retries deliveries, so the same payload has to be safe to apply twice.
    @Test
    void replayingTheSameWebhookLeavesTheAssetUntouched() {
        LoginResponse player = register("player@example.com");
        UploadIntentResponse intent = uploadIntent(player);

        sendWebhook(readyPayload(), Instant.now()).expectStatus().isNoContent();
        String firstUpdate = jdbc.queryForObject(
                "SELECT updated_at::text FROM video_asset WHERE id = ?::uuid", String.class,
                intent.videoAssetId().toString());

        sendWebhook(readyPayload(), Instant.now()).expectStatus().isNoContent();

        assertThat(assetStatus(intent.videoAssetId().toString())).isEqualTo("READY");
        assertThat(jdbc.queryForObject(
                "SELECT updated_at::text FROM video_asset WHERE id = ?::uuid", String.class,
                intent.videoAssetId().toString())).isEqualTo(firstUpdate);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM video_asset", Integer.class))
                .isEqualTo(1);
    }

    // A captured delivery must not stay replayable forever, so the signed timestamp is checked.
    @Test
    void aWebhookSignedTooLongAgoIsRefused() {
        LoginResponse player = register("player@example.com");
        UploadIntentResponse intent = uploadIntent(player);

        sendWebhook(readyPayload(), Instant.now().minusSeconds(3600))
                .expectStatus().isForbidden();

        assertThat(assetStatus(intent.videoAssetId().toString())).isEqualTo("UPLOADING");
    }

    @Test
    void thePlayerCanGetASignedPlaybackTokenForTheirOwnVideo() {
        LoginResponse player = register("player@example.com");
        SubmissionSummary submission = submit(player);

        PlaybackResponse playback = client.get()
                .uri("/api/v1/videos/{id}/playback", submission.videoAssetId())
                .header(HttpHeaders.AUTHORIZATION, bearer(player.accessToken()))
                .exchange()
                .expectStatus().isOk()
                .returnResult(PlaybackResponse.class)
                .getResponseBody();

        assertThat(playback.token()).isEqualTo("a-signed-token");
        assertThat(playback.hlsUrl()).contains("a-signed-token").endsWith(".m3u8");
        assertThat(playback.expiresAt()).isAfter(Instant.now());
    }

    // Step 7 of the plan's end-to-end run: a different player asking for this video gets a 403.
    @Test
    void aDifferentPlayerCannotGetAPlaybackTokenForSomebodyElsesVideo() {
        LoginResponse owner = register("owner@example.com");
        SubmissionSummary submission = submit(owner);

        LoginResponse stranger = register("stranger@example.com");

        client.get().uri("/api/v1/videos/{id}/playback", submission.videoAssetId())
                .header(HttpHeaders.AUTHORIZATION, bearer(stranger.accessToken()))
                .exchange()
                .expectStatus().isForbidden();
    }

    // A 404 here would turn the endpoint into a probe for which asset ids exist.
    @Test
    void anUnknownVideoIsForbiddenRatherThanNotFound() {
        LoginResponse player = register("player@example.com");

        client.get().uri("/api/v1/videos/{id}/playback", "11111111-1111-1111-1111-111111111111")
                .header(HttpHeaders.AUTHORIZATION, bearer(player.accessToken()))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void creatingASubmissionBeforeTheUploadLandsIsRejected() {
        LoginResponse player = register("player@example.com");
        UploadIntentResponse intent = uploadIntent(player);

        client.post().uri("/api/v1/submissions")
                .header(HttpHeaders.AUTHORIZATION, bearer(player.accessToken()))
                .body(new CreateSubmissionRequest(
                        intent.videoAssetId(), null, null))
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody(String.class)
                .value(body -> assertThat(body).contains("video-not-ready"));
    }

    @Test
    void aPlayerCannotAttachSomebodyElsesUpload() {
        LoginResponse owner = register("owner@example.com");
        UploadIntentResponse intent = uploadIntent(owner);
        sendWebhook(readyPayload(), Instant.now()).expectStatus().isNoContent();

        LoginResponse stranger = register("stranger@example.com");

        client.post().uri("/api/v1/submissions")
                .header(HttpHeaders.AUTHORIZATION, bearer(stranger.accessToken()))
                .body(new CreateSubmissionRequest(
                        intent.videoAssetId(), null, null))
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void aVideoCanOnlyBeSubmittedOnce() {
        LoginResponse player = register("player@example.com");
        UploadIntentResponse intent = uploadIntent(player);
        sendWebhook(readyPayload(), Instant.now()).expectStatus().isNoContent();
        createSubmission(player, intent.videoAssetId()).expectStatus().isCreated();

        createSubmission(player, intent.videoAssetId())
                .expectStatus().isEqualTo(409)
                .expectBody(String.class)
                .value(body -> assertThat(body).contains("already has a submission"));
    }

    @Test
    void aPlayerCanDiscardTheirOwnSubmissionBeforeACoachStarts() {
        LoginResponse player = register("player@example.com");
        SubmissionSummary submission = submit(player);

        client.delete().uri("/api/v1/submissions/{id}", submission.id())
                .header(HttpHeaders.AUTHORIZATION, bearer(player.accessToken()))
                .exchange()
                .expectStatus().isNoContent();

        assertThat(jdbc.queryForObject("SELECT count(*) FROM video_submission WHERE id = ?::uuid",
                Integer.class, submission.id().toString())).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM video_asset WHERE id = ?::uuid",
                Integer.class, submission.videoAssetId().toString())).isZero();
    }

    @Test
    void aPlayerCannotDiscardSomebodyElsesSubmission() {
        LoginResponse owner = register("owner@example.com");
        SubmissionSummary submission = submit(owner);
        LoginResponse stranger = register("stranger@example.com");

        client.delete().uri("/api/v1/submissions/{id}", submission.id())
                .header(HttpHeaders.AUTHORIZATION, bearer(stranger.accessToken()))
                .exchange()
                .expectStatus().isNotFound();

        assertThat(jdbc.queryForObject("SELECT count(*) FROM video_submission WHERE id = ?::uuid",
                Integer.class, submission.id().toString())).isOne();
    }

    // The app closed between the upload and the create-submission call.
    @Test
    void theSweepRemovesAnUploadThatNeverBecameASubmission() {
        LoginResponse player = register("player@example.com");
        UploadIntentResponse intent = uploadIntent(player);
        backdateAsset(intent.videoAssetId().toString());

        assertThat(uploads.sweepOrphanedUploads()).isEqualTo(1);

        assertThat(jdbc.queryForObject("SELECT count(*) FROM video_asset WHERE id = ?::uuid",
                Integer.class, intent.videoAssetId().toString())).isZero();
    }

    @Test
    void theSweepLeavesARecentUpload() {
        LoginResponse player = register("player@example.com");
        UploadIntentResponse intent = uploadIntent(player);

        assertThat(uploads.sweepOrphanedUploads()).isZero();
        assertThat(assetStatus(intent.videoAssetId().toString())).isEqualTo("UPLOADING");
    }

    @Test
    void theSweepLeavesAnUploadThatBecameASubmission() {
        LoginResponse player = register("player@example.com");
        SubmissionSummary submission = submit(player);
        backdateAsset(submission.videoAssetId().toString());

        assertThat(uploads.sweepOrphanedUploads()).isZero();
        assertThat(assetStatus(submission.videoAssetId().toString())).isEqualTo("READY");
    }

    @Test
    void aPlayerSeesTheirOwnSubmissionsOnly() {
        LoginResponse owner = register("owner@example.com");
        submit(owner);
        LoginResponse stranger = register("stranger@example.com");

        client.get().uri("/api/v1/submissions/mine")
                .header(HttpHeaders.AUTHORIZATION, bearer(stranger.accessToken()))
                .exchange()
                .expectStatus().isOk()
                .expectBody(List.class)
                .value(body -> assertThat(body).isEmpty());
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

    private RestTestClient.ResponseSpec createSubmission(LoginResponse session, UUID assetId) {
        return client.post().uri("/api/v1/submissions")
                .header(HttpHeaders.AUTHORIZATION, bearer(session.accessToken()))
                .body(new CreateSubmissionRequest(
                        assetId, "Elbow hurts on contact", null))
                .exchange();
    }

    private SubmissionSummary submit(LoginResponse session) {
        UploadIntentResponse intent = uploadIntent(session);
        sendWebhook(readyPayload(), Instant.now()).expectStatus().isNoContent();
        return createSubmission(session, intent.videoAssetId())
                .expectStatus().isCreated()
                .returnResult(SubmissionSummary.class)
                .getResponseBody();
    }

    private void backdateAsset(String assetId) {
        jdbc.update("UPDATE video_asset SET created_at = now() - interval '2 hours' "
                + "WHERE id = ?::uuid", assetId);
    }

    private RestTestClient.ResponseSpec sendWebhook(String payload, Instant signedAt) {
        long time = signedAt.getEpochSecond();
        return client.post().uri("/api/v1/webhooks/cloudflare-stream")
                .header("Webhook-Signature", "time=" + time + ",sig1=" + sign(time + "." + payload))
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .exchange();
    }

    private String readyPayload() {
        return ("{\"uid\":\"" + STREAM_UID + "\",\"status\":{\"state\":\"ready\"},"
                + "\"duration\":42.4,"
                + "\"thumbnail\":\"https://videodelivery.net/" + STREAM_UID
                + "/thumbnails/thumbnail.jpg\",\"size\":1234}");
    }

    private String assetStatus(String assetId) {
        return jdbc.queryForObject(
                "SELECT status FROM video_asset WHERE id = ?::uuid", String.class, assetId);
    }

    private static String sign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                    WEBHOOK_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
