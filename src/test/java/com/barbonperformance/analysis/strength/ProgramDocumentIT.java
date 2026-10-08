package com.barbonperformance.analysis.strength;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.barbonperformance.analysis.AbstractIntegrationTest;
import com.barbonperformance.analysis.player.application.dto.PlayerHomeDtos.PlayerHomeResponse;
import com.barbonperformance.analysis.strength.application.dto.ProgramDtos.ProgramPdfLinkView;
import com.barbonperformance.analysis.strength.application.dto.ProgramDtos.WriteTrainingProgramRequest;
import com.barbonperformance.analysis.strength.application.dto.RedFlagDtos.RedFlagInput;
import com.barbonperformance.analysis.strength.application.dto.RedFlagDtos.ReplaceRedFlagsRequest;
import com.barbonperformance.analysis.strength.application.dto.StrengthDtos.CyclePhaseInput;
import com.barbonperformance.analysis.strength.application.dto.StrengthDtos.DayExerciseInput;
import com.barbonperformance.analysis.strength.application.dto.StrengthDtos.DaySectionInput;
import com.barbonperformance.analysis.strength.application.dto.StrengthDtos.ReplaceCyclePhasesRequest;
import com.barbonperformance.analysis.strength.application.dto.StrengthDtos.ReplaceDaySectionsRequest;
import com.barbonperformance.analysis.strength.application.dto.StrengthDtos.ReplaceTrainingDaysRequest;
import com.barbonperformance.analysis.strength.application.dto.StrengthDtos.TrainingDayInput;
import com.barbonperformance.analysis.strength.application.dto.StrengthDtos.TrainingDayView;
import com.barbonperformance.analysis.strength.domain.TrainingDayKind;
import com.barbonperformance.analysis.user.domain.CoachProfile;
import com.barbonperformance.analysis.user.domain.PlayerProfile;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

class ProgramDocumentIT extends AbstractIntegrationTest {

    private static final String PASSWORD = "a-long-enough-password";

    @LocalServerPort
    private int port;

    private PlayerProfile player;
    private String coachAuth;
    private String playerAuth;
    private String otherCoachAuth;

    @BeforeEach
    void coachWithAClient() {
        CoachProfile coach = fixtures.createCoach("coach@example.com", PASSWORD, "Federico Barbon");
        player = fixtures.createPlayer("player@example.com", PASSWORD, "Bill Van Mater", coach);
        fixtures.createCoach("other-coach@example.com", PASSWORD, "Another Coach");
        coachAuth = bearerFor("coach@example.com", PASSWORD);
        playerAuth = bearerFor("player@example.com", PASSWORD);
        otherCoachAuth = bearerFor("other-coach@example.com", PASSWORD);
    }

    @Test
    void thePdfIsGeneratedFromTheProgramTheCoachBuilt() throws IOException {
        buildProgram();

        ProgramPdfLinkView link = linkFor(coachAuth, "?playerProfileId=" + player.getId());
        byte[] pdf = open(link.url());

        try (PDDocument document = Loader.loadPDF(pdf)) {
            String text = new PDFTextStripper().getText(document);
            assertThat(text)
                    .contains("TRAINING PROGRAM", "Bill Van Mater", "USTA 4.0", "Federico Barbon")
                    .contains("Sharp or electric pain", "Tennis longevity.", "Order the days however you want.")
                    .contains("Day 1 — Lower Body", "INJURY PREVENTION", "10-12 min", "Banded Clamshell",
                            "Knees bent, feet together", "2 × 12 ea", "Both sides.")
                    .contains("MAIN WORK", "Goblet Squat", "3×8 @ RPE 7", "2×8 @ RPE 6", "W3 Intensity")
                    .contains("Day 2 — Steady-State Cardio", "Stationary bike, 40 min continuous.")
                    .contains("RPE GUIDE", "RPE 4-5", "Recover");
            assertThat(text.split("3×10 @ RPE 7", -1)).as("an empty week repeats the week before it").hasSize(3);
            assertThat(linksIn(document)).hasSize(2)
                    .allMatch(url -> url.startsWith("http://localhost:8080/api/v1/public/exercise-videos/"));
        }
    }

    @Test
    void anExerciseVideoLinkRedirectsToAFreshPlaybackUrl() throws IOException {
        buildProgram();
        CLOUDFLARE.stubFor(post(urlPathEqualTo("/accounts/test-account/stream/squat-uid/token"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"success":true,"errors":[],"result":{"token":"a-signed-token"}}
                                """)));

        String squatLink;
        try (PDDocument document = Loader.loadPDF(open(linkFor(playerAuth, "").url()))) {
            squatLink = linksIn(document).get(1);
        }

        HttpResponse<Void> redirect = getWithoutFollowing(pathOf(squatLink));
        assertThat(redirect.statusCode()).isEqualTo(302);
        assertThat(redirect.headers().firstValue(HttpHeaders.LOCATION))
                .contains("https://videodelivery.net/a-signed-token/watch");

        assertThat(getWithoutFollowing(pathOf(squatLink) + "x").statusCode()).isEqualTo(404);
    }

    @Test
    void aTamperedOrForeignLinkOpensNothing() {
        buildProgram();
        String url = linkFor(coachAuth, "?playerProfileId=" + player.getId()).url();

        client.get().uri(pathOf(url).replace(".", "x."))
                .exchange()
                .expectStatus().isNotFound();

        client.get().uri("/api/v1/strength/program-pdf-link?playerProfileId=" + player.getId())
                .header(HttpHeaders.AUTHORIZATION, otherCoachAuth)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void thePlayersHomeLinksTheGeneratedPdfOnceThereAreDays() {
        assertThat(home().programPdfUrl()).isNull();

        buildProgram();

        assertThat(home().programPdfUrl()).startsWith("http://localhost:8080/api/v1/public/program-pdf/");
        assertThat(open(home().programPdfUrl())).startsWith("%PDF".getBytes());
    }

    private void buildProgram() {
        fixtures.createLibraryItem("Unused");
        var clamshell = fixtures.createReadyLibraryItem("Banded Clamshell", "Knees bent, feet together",
                "clamshell-uid");
        var squat = fixtures.createReadyLibraryItem("Goblet Squat", "Chest up", "squat-uid");

        client.put().uri("/api/v1/strength/players/{id}/program", player.getId())
                .header(HttpHeaders.AUTHORIZATION, coachAuth)
                .body(new WriteTrainingProgramRequest("USTA 4.0", "Tennis longevity.",
                        "Order the days however you want."))
                .exchange()
                .expectStatus().isOk();

        client.put().uri("/api/v1/strength/players/{id}/red-flags", player.getId())
                .header(HttpHeaders.AUTHORIZATION, coachAuth)
                .body(new ReplaceRedFlagsRequest(List.of(
                        new RedFlagInput("Sharp or electric pain", "Stop and message me."))))
                .exchange()
                .expectStatus().is2xxSuccessful();

        client.put().uri("/api/v1/strength/players/{id}/cycle-phases", player.getId())
                .header(HttpHeaders.AUTHORIZATION, coachAuth)
                .body(new ReplaceCyclePhasesRequest(List.of(
                        new CyclePhaseInput(1, "Base", "Learn the movements", "2 × 8-10", "RPE 5-6"),
                        new CyclePhaseInput(2, "Volume", "Build work capacity", "3 × 10-12", "RPE 6-7"),
                        new CyclePhaseInput(3, "Intensity", "Build strength", "3 × 6-8", "RPE 7-8"),
                        new CyclePhaseInput(4, "Deload", "Recover", "2 × 8", "RPE 4-5"))))
                .exchange()
                .expectStatus().isOk();

        List<TrainingDayView> days = client.put()
                .uri("/api/v1/strength/players/{id}/training-days", player.getId())
                .header(HttpHeaders.AUTHORIZATION, coachAuth)
                .body(new ReplaceTrainingDaysRequest(List.of(
                        new TrainingDayInput(null, "Lower Body", "RPE ceiling: 8", 45, TrainingDayKind.STRENGTH,
                                null),
                        new TrainingDayInput(null, "Steady-State Cardio", "Aerobic base", 45,
                                TrainingDayKind.CARDIO, "Stationary bike, 40 min continuous."))))
                .exchange()
                .expectStatus().isOk()
                .returnResult(new ParameterizedTypeReference<List<TrainingDayView>>() {
                })
                .getResponseBody();

        client.put().uri("/api/v1/strength/training-days/{id}/sections", days.get(0).id())
                .header(HttpHeaders.AUTHORIZATION, coachAuth)
                .body(new ReplaceDaySectionsRequest(List.of(
                        new DaySectionInput(null, "Injury prevention", "10-12 min", false, List.of(
                                new DayExerciseInput(null, clamshell.getId(), "2 × 12 ea", null, null, null, null,
                                        "Both sides."))),
                        new DaySectionInput(null, "Main work", null, true, List.of(
                                new DayExerciseInput(null, squat.getId(), "3×8 @ RPE 7", "3×10 @ RPE 7", null,
                                        "2×8 @ RPE 6", null, null))))))
                .exchange()
                .expectStatus().isOk();
    }

    private ProgramPdfLinkView linkFor(String auth, String query) {
        return client.get().uri("/api/v1/strength/program-pdf-link" + query)
                .header(HttpHeaders.AUTHORIZATION, auth)
                .exchange()
                .expectStatus().isOk()
                .returnResult(ProgramPdfLinkView.class)
                .getResponseBody();
    }

    private byte[] open(String url) {
        return client.get().uri(pathOf(url))
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_PDF)
                .returnResult(byte[].class)
                .getResponseBody();
    }

    private PlayerHomeResponse home() {
        return client.get().uri("/api/v1/players/me/home")
                .header(HttpHeaders.AUTHORIZATION, playerAuth)
                .exchange()
                .expectStatus().isOk()
                .returnResult(PlayerHomeResponse.class)
                .getResponseBody();
    }

    // The test client follows redirects, which would chase the playback URL out to the internet.
    private HttpResponse<Void> getWithoutFollowing(String path) throws IOException {
        try (HttpClient http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build()) {
            return http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).build(),
                    HttpResponse.BodyHandlers.discarding());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        }
    }

    // The links carry the configured public origin; the test server listens on a random port.
    private static String pathOf(String url) {
        return URI.create(url).getRawPath();
    }

    private static List<String> linksIn(PDDocument document) throws IOException {
        List<String> urls = new ArrayList<>();
        for (PDPage page : document.getPages()) {
            for (PDAnnotation annotation : page.getAnnotations()) {
                if (annotation instanceof PDAnnotationLink link && link.getAction() instanceof PDActionURI uri) {
                    urls.add(uri.getURI());
                }
            }
        }
        return urls;
    }
}
