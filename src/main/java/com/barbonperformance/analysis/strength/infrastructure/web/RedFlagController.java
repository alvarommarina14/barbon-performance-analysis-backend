package com.barbonperformance.analysis.strength.infrastructure.web;

import com.barbonperformance.analysis.auth.domain.AuthenticatedUser;
import com.barbonperformance.analysis.strength.application.RedFlagService;
import com.barbonperformance.analysis.strength.application.dto.RedFlagDtos.RedFlagView;
import com.barbonperformance.analysis.strength.application.dto.RedFlagDtos.RedFlagsView;
import com.barbonperformance.analysis.strength.application.dto.RedFlagDtos.ReplaceRedFlagsRequest;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/strength")
public class RedFlagController {

    private final RedFlagService redFlags;

    public RedFlagController(RedFlagService redFlags) {
        this.redFlags = redFlags;
    }

    @GetMapping("/red-flags")
    @Operation(summary = "Red flags for everyone, plus the named player's own")
    RedFlagsView redFlags(@AuthenticationPrincipal AuthenticatedUser principal,
            @RequestParam(required = false) UUID playerProfileId) {
        return redFlags.listForCaller(principal.id(), playerProfileId);
    }

    @PutMapping("/red-flags")
    @PreAuthorize("hasRole('COACH')")
    @Operation(summary = "Replace the red flags every client sees")
    List<RedFlagView> replaceForEveryone(@AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody ReplaceRedFlagsRequest request) {
        return redFlags.replaceForEveryone(principal.id(), request);
    }

    @PutMapping("/players/{playerProfileId}/red-flags")
    @PreAuthorize("hasRole('COACH')")
    @Operation(summary = "Replace one client's own red flags")
    List<RedFlagView> replaceForPlayer(@AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable UUID playerProfileId, @Valid @RequestBody ReplaceRedFlagsRequest request) {
        return redFlags.replaceForPlayer(principal.id(), playerProfileId, request);
    }
}
