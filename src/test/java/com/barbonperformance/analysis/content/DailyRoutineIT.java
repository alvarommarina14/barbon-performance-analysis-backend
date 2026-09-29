package com.barbonperformance.analysis.content;

import static org.assertj.core.api.Assertions.assertThat;

import com.barbonperformance.analysis.AbstractIntegrationTest;
import com.barbonperformance.analysis.content.application.dto.ContentDtos.ConfirmPdfUploadRequest;
import com.barbonperformance.analysis.content.application.dto.ContentDtos.PdfUploadIntentRequest;
import com.barbonperformance.analysis.content.application.dto.ContentDtos.PdfUploadIntentResponse;
import com.barbonperformance.analysis.content.application.dto.DailyRoutineDtos.DailyRoutineView;
import com.barbonperformance.analysis.user.domain.CoachProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

class DailyRoutineIT extends AbstractIntegrationTest {

    private static final String PASSWORD = "a-long-enough-password";
    private static final String PDF = "application/pdf";

    private String coachAuth;
    private String playerAuth;

    @BeforeEach
    void coachAndClient() {
        CoachProfile coach = fixtures.createCoach("coach@example.com", PASSWORD, "Marcelo Barbon");
        fixtures.createPlayer("player@example.com", PASSWORD, "Rosario Fiol", coach);
        coachAuth = bearerFor("coach@example.com", PASSWORD);
        playerAuth = bearerFor("player@example.com", PASSWORD);
    }

    @Test
    void nothingUploadedYetIsAnEmptyRoutineNotAnError() {
        DailyRoutineView routine = client.get().uri("/api/v1/content/daily-routine")
                .header(HttpHeaders.AUTHORIZATION, playerAuth)
                .exchange()
                .expectStatus().isOk()
                .returnResult(DailyRoutineView.class)
                .getResponseBody();

        assertThat(routine.warmup()).isNull();
        assertThat(routine.recovery()).isNull();
        assertThat(routine.pdfUrl()).isNull();
    }

    @Test
    void theCoachConfirmsThePdfTheIntentHandedOut() {
        PdfUploadIntentResponse intent = client.post().uri("/api/v1/content/daily-routine/pdf-upload-intent")
                .header(HttpHeaders.AUTHORIZATION, coachAuth)
                .body(new PdfUploadIntentRequest(2048, PDF))
                .exchange()
                .expectStatus().isOk()
                .returnResult(PdfUploadIntentResponse.class)
                .getResponseBody();

        DailyRoutineView routine = client.post().uri("/api/v1/content/daily-routine/pdf")
                .header(HttpHeaders.AUTHORIZATION, coachAuth)
                .body(new ConfirmPdfUploadRequest(intent.objectKey(), PDF, "daily-routine.pdf", 2048))
                .exchange()
                .expectStatus().isOk()
                .returnResult(DailyRoutineView.class)
                .getResponseBody();

        assertThat(routine.pdfUrl()).isNotBlank();
        assertThat(routine.pdfFileName()).isEqualTo("daily-routine.pdf");
        assertThat(routine.updatedAt()).isNotNull();
    }

    @Test
    void aKeyTheIntentDidNotHandOutIsRejected() {
        client.post().uri("/api/v1/content/daily-routine/pdf")
                .header(HttpHeaders.AUTHORIZATION, coachAuth)
                .body(new ConfirmPdfUploadRequest("content/someone-else/secret.pdf", PDF, "x.pdf", 10))
                .exchange()
                .expectStatus().isEqualTo(409);
    }

    @Test
    void thereIsNoPlaybackBeforeAVideoIsUploaded() {
        client.get().uri("/api/v1/content/daily-routine/playback?part=WARMUP")
                .header(HttpHeaders.AUTHORIZATION, playerAuth)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void aPlayerCannotUploadTheRoutine() {
        client.post().uri("/api/v1/content/daily-routine/pdf-upload-intent")
                .header(HttpHeaders.AUTHORIZATION, playerAuth)
                .body(new PdfUploadIntentRequest(2048, PDF))
                .exchange()
                .expectStatus().isForbidden();
    }
}
