package com.barbonperformance.analysis.player.infrastructure.web;

import com.barbonperformance.analysis.auth.domain.AuthenticatedUser;
import com.barbonperformance.analysis.player.application.PlayerHomeService;
import com.barbonperformance.analysis.player.application.ProgramPdfService;
import com.barbonperformance.analysis.player.application.dto.PlayerHomeDtos.PlayerHomeResponse;
import com.barbonperformance.analysis.player.application.dto.ProgramPdfDtos.ConfirmProgramPdfRequest;
import com.barbonperformance.analysis.player.application.dto.ProgramPdfDtos.ProgramPdfUploadIntentRequest;
import com.barbonperformance.analysis.player.application.dto.ProgramPdfDtos.ProgramPdfUploadIntentResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/players")
public class PlayerController {

    private final PlayerHomeService playerHome;
    private final ProgramPdfService programPdf;

    public PlayerController(PlayerHomeService playerHome, ProgramPdfService programPdf) {
        this.playerHome = playerHome;
        this.programPdf = programPdf;
    }

    @GetMapping("/me/home")
    @PreAuthorize("hasRole('PLAYER')")
    @Operation(summary = "The signed-in player's home screen")
    PlayerHomeResponse home(@AuthenticationPrincipal AuthenticatedUser principal) {
        return playerHome.home(principal.id());
    }

    @PostMapping("/{playerProfileId}/program-pdf/upload-intent")
    @PreAuthorize("hasRole('COACH')")
    @Operation(summary = "Reserve a one-time upload URL for a player's program PDF")
    ProgramPdfUploadIntentResponse programPdfUploadIntent(
            @AuthenticationPrincipal AuthenticatedUser principal, @PathVariable UUID playerProfileId,
            @Valid @RequestBody ProgramPdfUploadIntentRequest request) {
        return programPdf.requestUploadIntent(principal.id(), playerProfileId, request);
    }

    @PostMapping("/{playerProfileId}/program-pdf/confirm")
    @PreAuthorize("hasRole('COACH')")
    @Operation(summary = "Confirm a player's program PDF once it has finished uploading")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void confirmProgramPdf(@AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID playerProfileId, @Valid @RequestBody ConfirmProgramPdfRequest request) {
        programPdf.confirm(principal.id(), playerProfileId, request);
    }
}
