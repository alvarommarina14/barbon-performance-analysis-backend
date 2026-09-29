package com.barbonperformance.analysis.content.infrastructure.web;

import com.barbonperformance.analysis.auth.domain.AuthenticatedUser;
import com.barbonperformance.analysis.content.application.DailyRoutineService;
import com.barbonperformance.analysis.content.application.dto.ContentDtos.ConfirmPdfUploadRequest;
import com.barbonperformance.analysis.content.application.dto.ContentDtos.PdfUploadIntentRequest;
import com.barbonperformance.analysis.content.application.dto.ContentDtos.PdfUploadIntentResponse;
import com.barbonperformance.analysis.content.application.dto.ContentDtos.VideoUploadIntentResponse;
import com.barbonperformance.analysis.content.application.dto.DailyRoutineDtos.ConfirmRoutineVideoRequest;
import com.barbonperformance.analysis.content.application.dto.DailyRoutineDtos.DailyRoutinePlaybackView;
import com.barbonperformance.analysis.content.application.dto.DailyRoutineDtos.DailyRoutineView;
import com.barbonperformance.analysis.content.application.dto.DailyRoutineDtos.RoutineVideoUploadIntentRequest;
import com.barbonperformance.analysis.content.domain.DailyRoutinePart;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/content/daily-routine")
public class DailyRoutineController {

    private final DailyRoutineService routine;

    public DailyRoutineController(DailyRoutineService routine) {
        this.routine = routine;
    }

    @GetMapping
    @Operation(summary = "The warm-up and recovery videos and the daily routine PDF, if uploaded")
    DailyRoutineView get(@AuthenticationPrincipal AuthenticatedUser principal) {
        return routine.get(principal.id());
    }

    @GetMapping("/playback")
    @Operation(summary = "A short-lived signed playback token for the warm-up or recovery video")
    DailyRoutinePlaybackView playback(@AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam DailyRoutinePart part) {
        return routine.playback(principal.id(), part);
    }

    @PostMapping("/video-upload-intent")
    @PreAuthorize("hasRole('COACH')")
    @Operation(summary = "Reserve a one-time upload URL for the warm-up or recovery video")
    VideoUploadIntentResponse videoUploadIntent(@AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody RoutineVideoUploadIntentRequest request) {
        return routine.requestVideoUploadIntent(principal.id(), request);
    }

    @PostMapping("/video")
    @PreAuthorize("hasRole('COACH')")
    @Operation(summary = "Confirm the warm-up or recovery video once it has finished uploading")
    DailyRoutineView confirmVideo(@AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody ConfirmRoutineVideoRequest request) {
        return routine.confirmVideo(principal.id(), request);
    }

    @PostMapping("/pdf-upload-intent")
    @PreAuthorize("hasRole('COACH')")
    @Operation(summary = "Reserve a one-time upload URL for the daily routine PDF")
    PdfUploadIntentResponse pdfUploadIntent(@AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody PdfUploadIntentRequest request) {
        return routine.requestPdfUploadIntent(principal.id(), request);
    }

    @PostMapping("/pdf")
    @PreAuthorize("hasRole('COACH')")
    @Operation(summary = "Confirm the daily routine PDF once it has finished uploading")
    DailyRoutineView confirmPdf(@AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody ConfirmPdfUploadRequest request) {
        return routine.confirmPdf(principal.id(), request);
    }
}
