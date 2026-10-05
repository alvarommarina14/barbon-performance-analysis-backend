package com.barbonperformance.analysis.coach.application;

import com.barbonperformance.analysis.coach.application.dto.CoachDtos.BookingLinkView;
import com.barbonperformance.analysis.coach.application.dto.CoachDtos.ClientSummary;
import com.barbonperformance.analysis.coach.application.dto.CoachDtos.SetBookingLinkRequest;
import com.barbonperformance.analysis.shared.domain.NotFoundException;
import com.barbonperformance.analysis.user.domain.CoachProfile;
import com.barbonperformance.analysis.user.domain.CoachProfiles;
import com.barbonperformance.analysis.user.domain.PlanCalendar;
import com.barbonperformance.analysis.user.domain.PlayerProfiles;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CoachService {

    private final CoachProfiles coachProfiles;
    private final PlayerProfiles playerProfiles;

    public CoachService(CoachProfiles coachProfiles, PlayerProfiles playerProfiles) {
        this.coachProfiles = coachProfiles;
        this.playerProfiles = playerProfiles;
    }

    @Transactional(readOnly = true)
    public BookingLinkView bookingLink(UUID coachUserId) {
        return new BookingLinkView(coach(coachUserId).getBookingUrl());
    }

    @Transactional
    public BookingLinkView setBookingLink(UUID coachUserId, SetBookingLinkRequest request) {
        CoachProfile coach = coach(coachUserId);
        String url = request.bookingUrl() == null ? "" : request.bookingUrl().strip();
        coach.setBookingUrl(url.isEmpty() ? null : url);
        return new BookingLinkView(coachProfiles.save(coach).getBookingUrl());
    }

    private CoachProfile coach(UUID coachUserId) {
        return coachProfiles.findByUserId(coachUserId)
                .orElseThrow(() -> new NotFoundException("No coach profile for this account"));
    }

    @Transactional(readOnly = true)
    public List<ClientSummary> clients(UUID coachUserId) {
        UUID coachProfileId = coachProfiles.findByUserId(coachUserId)
                .orElseThrow(() -> new NotFoundException("No coach profile for this account"))
                .getId();

        return playerProfiles.findAllByCoachId(coachProfileId).stream()
                .map(profile -> new ClientSummary(
                        profile.getId(),
                        profile.getUser().getId(),
                        profile.getUser().getName(),
                        profile.getUser().getEmail(),
                        profile.getUser().getAvatarUrl(),
                        profile.getMembershipTier(),
                        PlanCalendar.planWeek(profile).orElse(null),
                        PlanCalendar.PLAN_WEEKS,
                        profile.getPlanStartedAt(),
                        profile.getNextCallAt()))
                .toList();
    }
}
