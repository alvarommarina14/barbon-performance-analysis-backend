package com.barbonperformance.analysis.strength.infrastructure.web;

import com.barbonperformance.analysis.strength.application.ProgramDocumentService;
import com.barbonperformance.analysis.strength.application.dto.ProgramDtos.RenderedProgramPdf;
import io.swagger.v3.oas.annotations.Operation;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// No bearer token reaches these: they are opened from a PDF or the phone's browser. The signed
// token in the path is the whole authorisation.
@RestController
@RequestMapping("/api/v1/public")
public class PublicProgramController {

    private final ProgramDocumentService documents;

    public PublicProgramController(ProgramDocumentService documents) {
        this.documents = documents;
    }

    @GetMapping(value = "/program-pdf/{token}", produces = MediaType.APPLICATION_PDF_VALUE)
    @Operation(summary = "The program PDF behind a signed link, generated from the current program")
    ResponseEntity<byte[]> programPdf(@PathVariable String token) {
        RenderedProgramPdf rendered = documents.render(token);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(rendered.fileName(), StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .body(rendered.pdf());
    }

    @GetMapping("/exercise-videos/{token}")
    @Operation(summary = "Redirect a program PDF's video link to a fresh playback URL")
    ResponseEntity<Void> exerciseVideo(@PathVariable String token) {
        return ResponseEntity.status(302)
                .cacheControl(CacheControl.noStore())
                .location(URI.create(documents.exerciseVideoUrl(token)))
                .build();
    }
}
