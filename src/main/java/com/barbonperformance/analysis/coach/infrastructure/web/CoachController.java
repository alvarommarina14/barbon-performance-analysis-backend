package com.barbonperformance.analysis.coach.infrastructure.web;

import com.barbonperformance.analysis.auth.domain.AuthenticatedUser;
import com.barbonperformance.analysis.coach.application.CoachService;
import com.barbonperformance.analysis.coach.application.dto.CoachDtos.BookingLinkView;
import com.barbonperformance.analysis.coach.application.dto.CoachDtos.ClientSummary;
import com.barbonperformance.analysis.coach.application.dto.CoachDtos.SetBookingLinkRequest;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/coach")
public class CoachController {

    private final CoachService coachService;

    public CoachController(CoachService coachService) {
        this.coachService = coachService;
    }

    @GetMapping("/booking-link")
    @PreAuthorize("hasRole('COACH')")
    @Operation(summary = "The link players open to book a call with the coach")
    BookingLinkView bookingLink(@AuthenticationPrincipal AuthenticatedUser principal) {
        return coachService.bookingLink(principal.id());
    }

    @PutMapping("/booking-link")
    @PreAuthorize("hasRole('COACH')")
    @Operation(summary = "Set or clear the link players open to book a call")
    BookingLinkView setBookingLink(@AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody SetBookingLinkRequest request) {
        return coachService.setBookingLink(principal.id(), request);
    }

    @GetMapping("/clients")
    @PreAuthorize("hasRole('COACH')")
    @Operation(summary = "The signed-in coach's clients")
    List<ClientSummary> clients(@AuthenticationPrincipal AuthenticatedUser principal) {
        return coachService.clients(principal.id());
    }
}
