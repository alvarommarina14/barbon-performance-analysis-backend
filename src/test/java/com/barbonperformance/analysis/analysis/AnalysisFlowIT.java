package com.barbonperformance.analysis.analysis;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.assertj.core.api.Assertions.assertThat;

import com.barbonperformance.analysis.AbstractIntegrationTest;
import com.barbonperformance.analysis.analysis.application.dto.AnalysisDtos.AnalysisDetail;
import com.barbonperformance.analysis.analysis.application.dto.AnalysisDtos.AnalysisSummary;
import com.barbonperformance.analysis.analysis.application.dto.AnalysisDtos.CreateAnalysisRequest;
import com.barbonperformance.analysis.analysis.application.dto.AnalysisDtos.FeedbackUploadIntentRequest;
import com.barbonperformance.analysis.analysis.application.dto.AnalysisDtos.FeedbackUploadIntentResponse;
import com.barbonperformance.analysis.analysis.application.dto.AnalysisDtos.FindingInput;
import com.barbonperformance.analysis.analysis.application.dto.AnalysisDtos.PlaybackView;
import com.barbonperformance.analysis.analysis.application.dto.AnalysisDtos.ReplaceFindingsRequest;
import com.barbonperformance.analysis.analysis.application.dto.AnalysisDtos.UpdateAnalysisRequest;
import com.barbonperformance.analysis.auth.application.dto.AuthDtos.LoginResponse;
import com.barbonperformance.analysis.auth.application.dto.AuthDtos.RegisterCoachRequest;
import com.barbonperformance.analysis.auth.application.dto.AuthDtos.RegisterRequest;
import com.barbonperformance.analysis.video.application.dto.VideoDtos.SubmissionSummary;
import com.barbonperformance.analysis.video.application.dto.VideoDtos.UploadIntentRequest;
import com.barbonperformance.analysis.video.application.dto.VideoDtos.UploadIntentResponse;
import com.barbonperformance.analysis.video.domain.StrokeType;
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

class AnalysisFlowIT extends AbstractIntegrationTest {

    private static final String PASSWORD = "a-long-enough-password";
    private static final String PLAYER_UID = "stream-player";
    private static final String FEEDBACK_UID = "stream-feedback";
    private static final String WEBHOOK_SECRET = "test-webhook-secret";
    private static final long SIZE_BYTES = 12_345_678L;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void stubCloudflare() {
        CLOUDFLARE.stubFor(post(urlPathEqualTo("/accounts/test-account/stream"))
                .withHeader("Upload-Metadata", containing("submissionId"))
                .willReturn(tusCreated(PLAYER_UID)));
        CLOUDFLARE.stubFor(post(urlPathEqualTo("/accounts/test-account/stream"))
                .withHeader("Upload-Metadata", containing("analysisId"))
                .willReturn(tusCreated(FEEDBACK_UID)));
        CLOUDFLARE.stubFor(post(urlPathMatching("/accounts/test-account/stream/.+/token"))
                .willReturn(json("""
                        {"success":true,"errors":[],"result":{"token":"a-signed-token"}}""")));
        CLOUDFLARE.stubFor(post(urlPathMatching("/accounts/test-account/stream/.+/downloads"))
                .willReturn(json("""
                        {"success":true,"errors":[],"result":{"default":{
                          "status":"ready","url":"https://videodelivery.net/original.mp4"}}}""")));
    }

    @Test
    void creatingFromASubmissionMovesItToInReview() {
        LoginResponse coach = registerCoach("coach@example.com");
        LoginResponse player = registerPlayer("player@example.com");
        UploadIntentResponse submission = readySubmission(player);

        AnalysisDetail analysis = createAnalysis(coach, submission.submissionId());

        assertThat(analysis.status().name()).isEqualTo("DRAFT");
        assertThat(analysis.strokeType()).isEqualTo(StrokeType.FOREHAND);
        assertThat(submissionStatus(submission.submissionId())).isEqualTo("IN_REVIEW");
    }

    @Test
    void feedbackUploadIntentAsksCloudflareForAPrivateVideoTaggedWithTheAnalysis() {
        LoginResponse coach = registerCoach("coach@example.com");
        LoginResponse player = registerPlayer("player@example.com");
        AnalysisDetail analysis = createAnalysis(coach, readySubmission(player).submissionId());

        FeedbackUploadIntentResponse intent = feedbackUploadIntent(coach, analysis.id());

        assertThat(intent.uploadUrl()).isEqualTo("https://upload.videodelivery.net/tus/" + FEEDBACK_UID);
        CLOUDFLARE.verify(postRequestedFor(urlPathEqualTo("/accounts/test-account/stream"))
                .withHeader("Upload-Metadata", containing("requiresignedurls"))
                .withHeader("Upload-Metadata", containing("analysisId")));
        assertThat(jdbc.queryForObject(
                "SELECT kind FROM video_asset WHERE id = ?::uuid", String.class,
                intent.videoAssetId().toString())).isEqualTo("COACH_FEEDBACK");
        assertThat(jdbc.queryForObject(
                "SELECT feedback_video_asset_id FROM analysis WHERE id = ?::uuid", String.class,
                analysis.id().toString())).isEqualTo(intent.videoAssetId().toString());
    }

    @Test
    void findingsAreBulkReplaced() {
        LoginResponse coach = registerCoach("coach@example.com");
        LoginResponse player = registerPlayer("player@example.com");
        AnalysisDetail analysis = createAnalysis(coach, readySubmission(player).submissionId());

        replaceFindings(coach, analysis.id(), List.of(
                new FindingInput(42, "Contact point", "behind the hip"),
                new FindingInput(255, "Trunk rotation", "all arm"),
                new FindingInput(570, "Recovery step", "planting flat")));

        AnalysisDetail after = replaceFindings(coach, analysis.id(), List.of(
                new FindingInput(10, "One", null),
                new FindingInput(20, "Two", "detail")));

        assertThat(after.findings()).hasSize(2);
        assertThat(after.findings().get(0).timestampSeconds()).isEqualTo(10);
        assertThat(after.findings().get(0).sortOrder()).isZero();
        assertThat(after.findings().get(1).sortOrder()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM analysis_finding", Integer.class))
                .isEqualTo(2);
    }

    @Test
    void publishIsRejectedUntilTheFeedbackVideoIsReady() {
        LoginResponse coach = registerCoach("coach@example.com");
        LoginResponse player = registerPlayer("player@example.com");
        AnalysisDetail analysis = createAnalysis(coach, readySubmission(player).submissionId());
        feedbackUploadIntent(coach, analysis.id());

        client.post().uri("/api/v1/analyses/{id}/publish", analysis.id())
                .header(HttpHeaders.AUTHORIZATION, bearer(coach.accessToken()))
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody(String.class)
                .value(body -> assertThat(body).contains("feedback-video-not-ready"));

        assertThat(analysisStatus(analysis.id())).isEqualTo("DRAFT");
    }

    @Test
    void publishSucceedsOnceTheFeedbackWebhookArrivesAndClosesTheSubmission() {
        LoginResponse coach = registerCoach("coach@example.com");
        LoginResponse player = registerPlayer("player@example.com");
        UploadIntentResponse submission = readySubmission(player);
        AnalysisDetail analysis = createAnalysis(coach, submission.submissionId());
        feedbackUploadIntent(coach, analysis.id());
        sendReadyWebhook(FEEDBACK_UID).expectStatus().isNoContent();

        AnalysisDetail published = publish(coach, analysis.id());

        assertThat(published.status().name()).isEqualTo("PUBLISHED");
        assertThat(published.publishedAt()).isNotNull();
        assertThat(submissionStatus(submission.submissionId())).isEqualTo("ANALYZED");
    }

    @Test
    void publishRemovesTheSubmissionFromTheCoachQueue() {
        LoginResponse coach = registerCoach("coach@example.com");
        LoginResponse player = registerPlayer("player@example.com");
        UploadIntentResponse submission = readySubmission(player);
        AnalysisDetail analysis = createAnalysis(coach, submission.submissionId());
        feedbackUploadIntent(coach, analysis.id());
        sendReadyWebhook(FEEDBACK_UID).expectStatus().isNoContent();

        assertThat(queueIds(coach)).contains(submission.submissionId());

        publish(coach, analysis.id());

        assertThat(queueIds(coach)).doesNotContain(submission.submissionId());
    }

    @Test
    void aReadySubmissionAppearsInTheCoachQueue() {
        LoginResponse coach = registerCoach("coach@example.com");
        LoginResponse player = registerPlayer("player@example.com");

        UploadIntentResponse ready = readySubmission(player);

        assertThat(queueIds(coach)).containsExactly(ready.submissionId());
    }

    @Test
    void anUnreadySubmissionIsNotInTheCoachQueue() {
        LoginResponse coach = registerCoach("coach@example.com");
        LoginResponse player = registerPlayer("player@example.com");

        uploadIntent(player);

        assertThat(queueIds(coach)).isEmpty();
    }

    @Test
    void aPlayerSeesOnlyTheirOwnPublishedAnalyses() {
        LoginResponse coach = registerCoach("coach@example.com");
        LoginResponse player = registerPlayer("player@example.com");
        LoginResponse other = registerPlayer("other@example.com");

        UploadIntentResponse submission = readySubmission(player);
        AnalysisDetail analysis = createAnalysis(coach, submission.submissionId());
        feedbackUploadIntent(coach, analysis.id());
        sendReadyWebhook(FEEDBACK_UID).expectStatus().isNoContent();
        publish(coach, analysis.id());

        // A second analysis the coach filmed themselves, left as a draft the player must not see.
        createAnalysisForPlayer(coach, playerProfileIdFor(player));

        assertThat(analysisIds(player)).containsExactly(analysis.id());
        assertThat(analysisIds(other)).isEmpty();
    }

    @Test
    void aPlayerGetsASignedPlaybackTokenForAPublishedAnalysis() {
        LoginResponse coach = registerCoach("coach@example.com");
        LoginResponse player = registerPlayer("player@example.com");
        AnalysisDetail analysis = createAnalysis(coach, readySubmission(player).submissionId());
        feedbackUploadIntent(coach, analysis.id());
        sendReadyWebhook(FEEDBACK_UID).expectStatus().isNoContent();
        publish(coach, analysis.id());

        PlaybackView playback = client.get()
                .uri("/api/v1/analyses/{id}/playback", analysis.id())
                .header(HttpHeaders.AUTHORIZATION, bearer(player.accessToken()))
                .exchange()
                .expectStatus().isOk()
                .returnResult(PlaybackView.class)
                .getResponseBody();

        assertThat(playback.token()).isEqualTo("a-signed-token");
        assertThat(playback.hlsUrl()).contains("a-signed-token").endsWith(".m3u8");
        assertThat(playback.expiresAt()).isAfter(Instant.now());
    }

    @Test
    void aPlayerCannotGetPlaybackForADraftAnalysis() {
        LoginResponse coach = registerCoach("coach@example.com");
        LoginResponse player = registerPlayer("player@example.com");
        AnalysisDetail analysis = createAnalysis(coach, readySubmission(player).submissionId());
        feedbackUploadIntent(coach, analysis.id());
        sendReadyWebhook(FEEDBACK_UID).expectStatus().isNoContent();

        client.get().uri("/api/v1/analyses/{id}/playback", analysis.id())
                .header(HttpHeaders.AUTHORIZATION, bearer(player.accessToken()))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void theCoachCanPreviewTheFeedbackVideoOnADraft() {
        LoginResponse coach = registerCoach("coach@example.com");
        LoginResponse player = registerPlayer("player@example.com");
        AnalysisDetail analysis = createAnalysis(coach, readySubmission(player).submissionId());
        feedbackUploadIntent(coach, analysis.id());
        sendReadyWebhook(FEEDBACK_UID).expectStatus().isNoContent();

        client.get().uri("/api/v1/analyses/{id}/playback", analysis.id())
                .header(HttpHeaders.AUTHORIZATION, bearer(coach.accessToken()))
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void editingAPublishedAnalysisIsRejected() {
        LoginResponse coach = registerCoach("coach@example.com");
        LoginResponse player = registerPlayer("player@example.com");
        AnalysisDetail analysis = createAnalysis(coach, readySubmission(player).submissionId());
        feedbackUploadIntent(coach, analysis.id());
        sendReadyWebhook(FEEDBACK_UID).expectStatus().isNoContent();
        publish(coach, analysis.id());

        client.put().uri("/api/v1/analyses/{id}", analysis.id())
                .header(HttpHeaders.AUTHORIZATION, bearer(coach.accessToken()))
                .body(new UpdateAnalysisRequest("new title", null))
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody(String.class)
                .value(body -> assertThat(body).contains("analysis-not-editable"));
    }

    @Test
    void creatingWithBothOrNeitherTargetIsRejected() {
        LoginResponse coach = registerCoach("coach@example.com");
        LoginResponse player = registerPlayer("player@example.com");
        UUID submissionId = readySubmission(player).submissionId();

        client.post().uri("/api/v1/analyses")
                .header(HttpHeaders.AUTHORIZATION, bearer(coach.accessToken()))
                .body(new CreateAnalysisRequest(submissionId, playerProfileIdFor(player), "t", null))
                .exchange()
                .expectStatus().isBadRequest();

        client.post().uri("/api/v1/analyses")
                .header(HttpHeaders.AUTHORIZATION, bearer(coach.accessToken()))
                .body(new CreateAnalysisRequest(null, null, "t", null))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void aCoachCannotAnalyseAPlayerWhoIsNotTheirClient() {
        // Registered before any coach exists, so this player has no coach.
        LoginResponse orphan = registerPlayer("orphan@example.com");
        LoginResponse coach = registerCoach("coach@example.com");

        client.post().uri("/api/v1/analyses")
                .header(HttpHeaders.AUTHORIZATION, bearer(coach.accessToken()))
                .body(new CreateAnalysisRequest(null, playerProfileIdFor(orphan), "t", null))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void analysingANonexistentPlayerIsNotFound() {
        LoginResponse coach = registerCoach("coach@example.com");

        client.post().uri("/api/v1/analyses")
                .header(HttpHeaders.AUTHORIZATION, bearer(coach.accessToken()))
                .body(new CreateAnalysisRequest(null, UUID.randomUUID(), "t", null))
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void deletingADraftReturnsTheSubmissionToTheQueue() {
        LoginResponse coach = registerCoach("coach@example.com");
        LoginResponse player = registerPlayer("player@example.com");
        UploadIntentResponse submission = readySubmission(player);
        AnalysisDetail analysis = createAnalysis(coach, submission.submissionId());
        assertThat(submissionStatus(submission.submissionId())).isEqualTo("IN_REVIEW");

        client.delete().uri("/api/v1/analyses/{id}", analysis.id())
                .header(HttpHeaders.AUTHORIZATION, bearer(coach.accessToken()))
                .exchange()
                .expectStatus().isNoContent();

        assertThat(submissionStatus(submission.submissionId())).isEqualTo("SUBMITTED");
        assertThat(queueIds(coach)).contains(submission.submissionId());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM analysis", Integer.class)).isZero();
    }

    @Test
    void coachCanCreateAnAnalysisWithoutASubmission() {
        LoginResponse coach = registerCoach("coach@example.com");
        LoginResponse player = registerPlayer("player@example.com");

        AnalysisDetail analysis = client.post().uri("/api/v1/analyses")
                .header(HttpHeaders.AUTHORIZATION, bearer(coach.accessToken()))
                .body(new CreateAnalysisRequest(null, playerProfileIdFor(player), "Filmed courtside", null))
                .exchange()
                .expectStatus().isCreated()
                .returnResult(AnalysisDetail.class)
                .getResponseBody();

        assertThat(analysis.submissionId()).isNull();
        assertThat(analysis.strokeType()).isNull();
        assertThat(analysis.playerNote()).isNull();
    }

    @Test
    void publishingTwiceIsRejected() {
        LoginResponse coach = registerCoach("coach@example.com");
        LoginResponse player = registerPlayer("player@example.com");
        AnalysisDetail analysis = createAnalysis(coach, readySubmission(player).submissionId());
        feedbackUploadIntent(coach, analysis.id());
        sendReadyWebhook(FEEDBACK_UID).expectStatus().isNoContent();
        publish(coach, analysis.id());

        client.post().uri("/api/v1/analyses/{id}/publish", analysis.id())
                .header(HttpHeaders.AUTHORIZATION, bearer(coach.accessToken()))
                .exchange()
                .expectStatus().isEqualTo(409);
    }

    @Test
    void anUnknownAnalysisIsNotFoundForACoach() {
        LoginResponse coach = registerCoach("coach@example.com");

        client.get().uri("/api/v1/analyses/{id}", UUID.randomUUID())
                .header(HttpHeaders.AUTHORIZATION, bearer(coach.accessToken()))
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void theCoachCanDownloadTheOriginalVideo() {
        LoginResponse coach = registerCoach("coach@example.com");
        LoginResponse player = registerPlayer("player@example.com");
        UploadIntentResponse submission = readySubmission(player);

        client.get().uri("/api/v1/videos/{id}/download-url", submission.videoAssetId())
                .header(HttpHeaders.AUTHORIZATION, bearer(coach.accessToken()))
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> {
                    assertThat(body).contains("original.mp4");
                    assertThat(body).contains("\"ready\":true");
                });
    }

    @Test
    void aStrangerCannotDownloadTheOriginalVideo() {
        LoginResponse coach = registerCoach("coach@example.com");
        LoginResponse player = registerPlayer("player@example.com");
        LoginResponse stranger = registerPlayer("stranger@example.com");
        UploadIntentResponse submission = readySubmission(player);

        client.get().uri("/api/v1/videos/{id}/download-url", submission.videoAssetId())
                .header(HttpHeaders.AUTHORIZATION, bearer(stranger.accessToken()))
                .exchange()
                .expectStatus().isForbidden();
    }

    // --- helpers ---------------------------------------------------------------

    private LoginResponse registerCoach(String email) {
        return client.post().uri("/api/v1/auth/register-coach")
                .body(new RegisterCoachRequest(email, PASSWORD, "Marcelo Barbon", null))
                .exchange()
                .expectStatus().isCreated()
                .returnResult(LoginResponse.class)
                .getResponseBody();
    }

    private LoginResponse registerPlayer(String email) {
        return client.post().uri("/api/v1/auth/register")
                .body(new RegisterRequest(email, PASSWORD, "Rosario Fiol", null))
                .exchange()
                .expectStatus().isCreated()
                .returnResult(LoginResponse.class)
                .getResponseBody();
    }

    private UUID playerProfileIdFor(LoginResponse player) {
        return player.user().player().profileId();
    }

    private UploadIntentResponse uploadIntent(LoginResponse player) {
        return client.post().uri("/api/v1/submissions/upload-intent")
                .header(HttpHeaders.AUTHORIZATION, bearer(player.accessToken()))
                .body(new UploadIntentRequest(
                        StrokeType.FOREHAND, "Elbow hurts on contact", null, SIZE_BYTES))
                .exchange()
                .expectStatus().isCreated()
                .returnResult(UploadIntentResponse.class)
                .getResponseBody();
    }

    private UploadIntentResponse readySubmission(LoginResponse player) {
        UploadIntentResponse intent = uploadIntent(player);
        sendReadyWebhook(PLAYER_UID).expectStatus().isNoContent();
        return intent;
    }

    private AnalysisDetail createAnalysis(LoginResponse coach, UUID submissionId) {
        return client.post().uri("/api/v1/analyses")
                .header(HttpHeaders.AUTHORIZATION, bearer(coach.accessToken()))
                .body(new CreateAnalysisRequest(submissionId, null, "Forehand & serve breakdown", "watch it through"))
                .exchange()
                .expectStatus().isCreated()
                .returnResult(AnalysisDetail.class)
                .getResponseBody();
    }

    private AnalysisDetail createAnalysisForPlayer(LoginResponse coach, UUID playerProfileId) {
        return client.post().uri("/api/v1/analyses")
                .header(HttpHeaders.AUTHORIZATION, bearer(coach.accessToken()))
                .body(new CreateAnalysisRequest(null, playerProfileId, "Filmed courtside", null))
                .exchange()
                .expectStatus().isCreated()
                .returnResult(AnalysisDetail.class)
                .getResponseBody();
    }

    private FeedbackUploadIntentResponse feedbackUploadIntent(LoginResponse coach, UUID analysisId) {
        return client.post().uri("/api/v1/analyses/{id}/feedback-upload-intent", analysisId)
                .header(HttpHeaders.AUTHORIZATION, bearer(coach.accessToken()))
                .body(new FeedbackUploadIntentRequest(SIZE_BYTES))
                .exchange()
                .expectStatus().isCreated()
                .returnResult(FeedbackUploadIntentResponse.class)
                .getResponseBody();
    }

    private AnalysisDetail replaceFindings(LoginResponse coach, UUID analysisId, List<FindingInput> findings) {
        return client.put().uri("/api/v1/analyses/{id}/findings", analysisId)
                .header(HttpHeaders.AUTHORIZATION, bearer(coach.accessToken()))
                .body(new ReplaceFindingsRequest(findings))
                .exchange()
                .expectStatus().isOk()
                .returnResult(AnalysisDetail.class)
                .getResponseBody();
    }

    private AnalysisDetail publish(LoginResponse coach, UUID analysisId) {
        return client.post().uri("/api/v1/analyses/{id}/publish", analysisId)
                .header(HttpHeaders.AUTHORIZATION, bearer(coach.accessToken()))
                .exchange()
                .expectStatus().isOk()
                .returnResult(AnalysisDetail.class)
                .getResponseBody();
    }

    private List<UUID> queueIds(LoginResponse coach) {
        SubmissionSummary[] queue = client.get().uri("/api/v1/submissions/queue")
                .header(HttpHeaders.AUTHORIZATION, bearer(coach.accessToken()))
                .exchange()
                .expectStatus().isOk()
                .returnResult(SubmissionSummary[].class)
                .getResponseBody();
        return List.of(queue).stream().map(SubmissionSummary::id).toList();
    }

    private List<UUID> analysisIds(LoginResponse session) {
        AnalysisSummary[] summaries = client.get().uri("/api/v1/analyses")
                .header(HttpHeaders.AUTHORIZATION, bearer(session.accessToken()))
                .exchange()
                .expectStatus().isOk()
                .returnResult(AnalysisSummary[].class)
                .getResponseBody();
        return List.of(summaries).stream().map(AnalysisSummary::id).toList();
    }

    private String analysisStatus(UUID analysisId) {
        return jdbc.queryForObject(
                "SELECT status FROM analysis WHERE id = ?::uuid", String.class, analysisId.toString());
    }

    private String submissionStatus(UUID submissionId) {
        return jdbc.queryForObject(
                "SELECT status FROM video_submission WHERE id = ?::uuid", String.class,
                submissionId.toString());
    }

    private RestTestClient.ResponseSpec sendReadyWebhook(String streamUid) {
        String payload = "{\"uid\":\"" + streamUid + "\",\"status\":{\"state\":\"ready\"},"
                + "\"duration\":42.4,\"thumbnail\":\"https://videodelivery.net/" + streamUid
                + "/thumbnails/thumbnail.jpg\",\"size\":1234}";
        long time = Instant.now().getEpochSecond();
        return client.post().uri("/api/v1/webhooks/cloudflare-stream")
                .header("Webhook-Signature", "time=" + time + ",sig1=" + sign(time + "." + payload))
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .exchange();
    }

    private static com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder tusCreated(String uid) {
        return aResponse().withStatus(201)
                .withHeader("Location", "https://upload.videodelivery.net/tus/" + uid)
                .withHeader("stream-media-id", uid);
    }

    private static com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder json(String body) {
        return aResponse().withHeader("Content-Type", "application/json").withBody(body);
    }

    private static String sign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(WEBHOOK_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
