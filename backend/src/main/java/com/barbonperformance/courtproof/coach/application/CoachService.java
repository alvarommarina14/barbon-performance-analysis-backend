package com.barbonperformance.courtproof.coach.application;

import com.barbonperformance.courtproof.coach.application.dto.CoachDtos.ClientSummary;
import com.barbonperformance.courtproof.shared.domain.NotFoundException;
import com.barbonperformance.courtproof.user.domain.CoachProfiles;
import com.barbonperformance.courtproof.user.domain.PlayerProfiles;
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
                        profile.getCurrentWeek(),
                        profile.getNextCallAt()))
                .toList();
    }
}
