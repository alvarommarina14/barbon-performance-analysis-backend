package com.barbonperformance.analysis.strength.application;

import com.barbonperformance.analysis.shared.domain.NotFoundException;
import com.barbonperformance.analysis.user.domain.AppUsers;
import com.barbonperformance.analysis.user.domain.CoachProfile;
import com.barbonperformance.analysis.user.domain.CoachProfiles;
import com.barbonperformance.analysis.user.domain.PlayerProfile;
import com.barbonperformance.analysis.user.domain.PlayerProfiles;
import com.barbonperformance.analysis.user.domain.UserRole;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
class PlayerAccess {

    private final AppUsers users;
    private final PlayerProfiles playerProfiles;
    private final CoachProfiles coachProfiles;

    PlayerAccess(AppUsers users, PlayerProfiles playerProfiles, CoachProfiles coachProfiles) {
        this.users = users;
        this.playerProfiles = playerProfiles;
        this.coachProfiles = coachProfiles;
    }

    // Another coach's client answers 404, not 403, so ids cannot be probed across coaches.
    PlayerProfile ownedByCoach(UUID coachUserId, UUID playerProfileId) {
        CoachProfile coach = coach(coachUserId);
        PlayerProfile player = playerProfiles.findById(playerProfileId)
                .orElseThrow(() -> new NotFoundException("Player " + playerProfileId + " not found"));
        if (player.getCoach() == null || !player.getCoach().getId().equals(coach.getId())) {
            throw new NotFoundException("Player " + playerProfileId + " not found");
        }
        return player;
    }

    PlayerProfile readableBy(UUID userId, UUID playerProfileId) {
        if (role(userId) == UserRole.PLAYER) {
            PlayerProfile self = ownPlayer(userId);
            if (playerProfileId != null && !playerProfileId.equals(self.getId())) {
                throw new AccessDeniedException("Not entitled to another player's data");
            }
            return self;
        }
        if (playerProfileId == null) {
            throw new NotFoundException("Name the player with playerProfileId");
        }
        return ownedByCoach(userId, playerProfileId);
    }

    UserRole role(UUID userId) {
        return users.findById(userId)
                .orElseThrow(() -> new NotFoundException("User " + userId + " not found"))
                .getRole();
    }

    PlayerProfile ownPlayer(UUID playerUserId) {
        return playerProfiles.findByUserId(playerUserId)
                .orElseThrow(() -> new NotFoundException("No player profile for this account"));
    }

    CoachProfile coach(UUID coachUserId) {
        return coachProfiles.findByUserId(coachUserId)
                .orElseThrow(() -> new NotFoundException("No coach profile for this account"));
    }
}
