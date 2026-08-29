package com.barbonperformance.courtproof.player.application;

import com.barbonperformance.courtproof.player.application.dto.PlayerHomeDtos.CoachCard;
import com.barbonperformance.courtproof.player.application.dto.PlayerHomeDtos.PlayerHomeResponse;
import com.barbonperformance.courtproof.shared.domain.NotFoundException;
import com.barbonperformance.courtproof.user.domain.CoachProfile;
import com.barbonperformance.courtproof.user.domain.PlayerProfile;
import com.barbonperformance.courtproof.user.domain.PlayerProfiles;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlayerHomeService {

    private final PlayerProfiles playerProfiles;

    public PlayerHomeService(PlayerProfiles playerProfiles) {
        this.playerProfiles = playerProfiles;
    }

    @Transactional(readOnly = true)
    public PlayerHomeResponse home(UUID userId) {
        PlayerProfile profile = playerProfiles.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("No player profile for this account"));

        CoachProfile coach = profile.getCoach();
        return new PlayerHomeResponse(
                profile.getId(),
                profile.getUser().getName(),
                profile.getUser().getAvatarUrl(),
                profile.getMembershipTier(),
                profile.getCurrentWeek(),
                PlayerProfile.CYCLE_WEEKS,
                profile.getCycleStartedAt(),
                profile.getNextCallAt(),
                coach == null ? null : new CoachCard(
                        coach.getId(),
                        coach.getUser().getName(),
                        coach.getUser().getAvatarUrl(),
                        coach.getBookingUrl()));
    }
}
