package com.barbonperformance.courtproof.player.controller;

import com.barbonperformance.courtproof.auth.domain.AuthenticatedUser;
import com.barbonperformance.courtproof.player.application.PlayerHomeService;
import com.barbonperformance.courtproof.player.dto.PlayerHomeDtos.PlayerHomeResponse;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/players")
public class PlayerController {

    private final PlayerHomeService playerHome;

    public PlayerController(PlayerHomeService playerHome) {
        this.playerHome = playerHome;
    }

    @GetMapping("/me/home")
    @PreAuthorize("hasRole('PLAYER')")
    @Operation(summary = "The signed-in player's home screen")
    PlayerHomeResponse home(@AuthenticationPrincipal AuthenticatedUser principal) {
        return playerHome.home(principal.id());
    }
}
