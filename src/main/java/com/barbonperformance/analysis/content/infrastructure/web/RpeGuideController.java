package com.barbonperformance.analysis.content.infrastructure.web;

import com.barbonperformance.analysis.auth.domain.AuthenticatedUser;
import com.barbonperformance.analysis.content.application.RpeGuideService;
import com.barbonperformance.analysis.content.application.dto.ContentDtos.ConfirmPdfUploadRequest;
import com.barbonperformance.analysis.content.application.dto.ContentDtos.PdfUploadIntentRequest;
import com.barbonperformance.analysis.content.application.dto.ContentDtos.PdfUploadIntentResponse;
import com.barbonperformance.analysis.content.application.dto.ContentDtos.RpeGuideView;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/content/rpe-guide")
public class RpeGuideController {

    private final RpeGuideService guide;

    public RpeGuideController(RpeGuideService guide) {
        this.guide = guide;
    }

    @GetMapping
    @Operation(summary = "The RPE guide PDF, if it has been uploaded")
    RpeGuideView get(@AuthenticationPrincipal AuthenticatedUser principal) {
        return guide.get(principal.id());
    }

    @PostMapping("/pdf-upload-intent")
    @PreAuthorize("hasRole('COACH')")
    @Operation(summary = "Reserve a one-time upload URL for the RPE guide PDF")
    PdfUploadIntentResponse pdfUploadIntent(@AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody PdfUploadIntentRequest request) {
        return guide.requestPdfUploadIntent(principal.id(), request);
    }

    @PostMapping("/pdf")
    @PreAuthorize("hasRole('COACH')")
    @Operation(summary = "Confirm the RPE guide PDF once it has finished uploading")
    RpeGuideView confirmPdf(@AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody ConfirmPdfUploadRequest request) {
        return guide.confirmPdf(principal.id(), request);
    }
}
