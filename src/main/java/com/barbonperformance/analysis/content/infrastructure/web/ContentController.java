package com.barbonperformance.analysis.content.infrastructure.web;

import com.barbonperformance.analysis.auth.domain.AuthenticatedUser;
import com.barbonperformance.analysis.content.application.RecordingTipsService;
import com.barbonperformance.analysis.content.application.dto.ContentDtos.ConfirmPdfRequest;
import com.barbonperformance.analysis.content.application.dto.ContentDtos.ConfirmVideoRequest;
import com.barbonperformance.analysis.content.application.dto.ContentDtos.PdfUploadIntentRequest;
import com.barbonperformance.analysis.content.application.dto.ContentDtos.PdfUploadIntentResponse;
import com.barbonperformance.analysis.content.application.dto.ContentDtos.RecordingTipsPlaybackView;
import com.barbonperformance.analysis.content.application.dto.ContentDtos.RecordingTipsView;
import com.barbonperformance.analysis.content.application.dto.ContentDtos.VideoUploadIntentRequest;
import com.barbonperformance.analysis.content.application.dto.ContentDtos.VideoUploadIntentResponse;
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
@RequestMapping("/api/v1/content/recording-tips")
public class ContentController {

    private final RecordingTipsService tips;

    public ContentController(RecordingTipsService tips) {
        this.tips = tips;
    }

    @GetMapping
    @Operation(summary = "The recording-tips video and PDF, if they have been uploaded")
    RecordingTipsView get(@AuthenticationPrincipal AuthenticatedUser principal) {
        return tips.get(principal.id());
    }

    @GetMapping("/playback")
    @Operation(summary = "A short-lived signed playback token for the recording-tips video")
    RecordingTipsPlaybackView playback(@AuthenticationPrincipal AuthenticatedUser principal) {
        return tips.playback(principal.id());
    }

    @PostMapping("/video-upload-intent")
    @PreAuthorize("hasRole('COACH')")
    @Operation(summary = "Reserve a one-time upload URL for the recording-tips video")
    VideoUploadIntentResponse videoUploadIntent(@AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody VideoUploadIntentRequest request) {
        return tips.requestVideoUploadIntent(principal.id(), request);
    }

    @PostMapping("/video")
    @PreAuthorize("hasRole('COACH')")
    @Operation(summary = "Confirm the recording-tips video once it has finished uploading")
    RecordingTipsView confirmVideo(@AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody ConfirmVideoRequest request) {
        return tips.confirmVideo(principal.id(), request);
    }

    @PostMapping("/pdf-upload-intent")
    @PreAuthorize("hasRole('COACH')")
    @Operation(summary = "Reserve a one-time upload URL for the recording-tips PDF")
    PdfUploadIntentResponse pdfUploadIntent(@AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody PdfUploadIntentRequest request) {
        return tips.requestPdfUploadIntent(principal.id(), request);
    }

    @PostMapping("/pdf")
    @PreAuthorize("hasRole('COACH')")
    @Operation(summary = "Confirm the recording-tips PDF once it has finished uploading")
    RecordingTipsView confirmPdf(@AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody ConfirmPdfRequest request) {
        return tips.confirmPdf(principal.id(), request);
    }
}
