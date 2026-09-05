package com.barbonperformance.analysis.auth.infrastructure.web;

import com.barbonperformance.analysis.auth.application.AuthService;
import com.barbonperformance.analysis.auth.application.dto.AuthDtos.CurrentUserResponse;
import com.barbonperformance.analysis.auth.application.dto.AuthDtos.LoginRequest;
import com.barbonperformance.analysis.auth.application.dto.AuthDtos.LoginResponse;
import com.barbonperformance.analysis.auth.application.dto.AuthDtos.RefreshRequest;
import com.barbonperformance.analysis.auth.application.dto.AuthDtos.RegisterCoachRequest;
import com.barbonperformance.analysis.auth.application.dto.AuthDtos.RegisterRequest;
import com.barbonperformance.analysis.auth.application.dto.AuthDtos.TokenPairResponse;
import com.barbonperformance.analysis.auth.domain.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @SecurityRequirements
    @Operation(summary = "Register a player and start a session")
    ResponseEntity<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/register-coach")
    @SecurityRequirements
    @Operation(summary = "Register the organisation's coach and start a session")
    ResponseEntity<LoginResponse> registerCoach(@Valid @RequestBody RegisterCoachRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerCoach(request));
    }

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "Exchange email and password for a token pair")
    LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    @SecurityRequirements
    @Operation(summary = "Rotate a refresh token for a new pair")
    TokenPairResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request.refreshToken());
    }

    @PostMapping("/logout")
    @SecurityRequirements
    @Operation(summary = "Revoke a refresh token")
    ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    @Operation(summary = "The signed-in user and their profile")
    CurrentUserResponse me(@AuthenticationPrincipal AuthenticatedUser principal) {
        return authService.currentUser(principal.id());
    }
}
