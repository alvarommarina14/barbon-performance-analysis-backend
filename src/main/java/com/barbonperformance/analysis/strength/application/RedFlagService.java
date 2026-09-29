package com.barbonperformance.analysis.strength.application;

import com.barbonperformance.analysis.strength.application.dto.RedFlagDtos.RedFlagInput;
import com.barbonperformance.analysis.strength.application.dto.RedFlagDtos.RedFlagView;
import com.barbonperformance.analysis.strength.application.dto.RedFlagDtos.RedFlagsView;
import com.barbonperformance.analysis.strength.application.dto.RedFlagDtos.ReplaceRedFlagsRequest;
import com.barbonperformance.analysis.strength.domain.RedFlag;
import com.barbonperformance.analysis.strength.domain.RedFlags;
import com.barbonperformance.analysis.user.domain.CoachProfile;
import com.barbonperformance.analysis.user.domain.PlayerProfile;
import com.barbonperformance.analysis.user.domain.UserRole;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RedFlagService {

    private final RedFlags redFlags;
    private final PlayerAccess access;

    public RedFlagService(RedFlags redFlags, PlayerAccess access) {
        this.redFlags = redFlags;
        this.access = access;
    }

    // A coach who names no player gets the organisation-wide list alone, for the Setup screen.
    @Transactional(readOnly = true)
    public RedFlagsView listForCaller(UUID userId, UUID playerProfileId) {
        if (playerProfileId == null && access.role(userId) != UserRole.PLAYER) {
            UUID orgId = access.coach(userId).getOrganization().getId();
            return new RedFlagsView(describe(redFlags.findAllForEveryone(orgId)), List.of());
        }
        PlayerProfile player = access.readableBy(userId, playerProfileId);
        return new RedFlagsView(
                describe(redFlags.findAllForEveryone(player.getOrganization().getId())),
                describe(redFlags.findAllByPlayerId(player.getId())));
    }

    @Transactional
    public List<RedFlagView> replaceForEveryone(UUID coachUserId, ReplaceRedFlagsRequest request) {
        CoachProfile coach = access.coach(coachUserId);
        redFlags.deleteAllForEveryone(coach.getOrganization().getId());

        List<RedFlag> toSave = new ArrayList<>();
        int order = 0;
        for (RedFlagInput input : request.flags()) {
            toSave.add(RedFlag.forEveryone(coach.getOrganization(), input.title().strip(), blankToNull(input.body()),
                    order++));
        }
        return describe(redFlags.saveAll(toSave));
    }

    @Transactional
    public List<RedFlagView> replaceForPlayer(UUID coachUserId, UUID playerProfileId,
            ReplaceRedFlagsRequest request) {
        PlayerProfile player = access.ownedByCoach(coachUserId, playerProfileId);
        redFlags.deleteAllByPlayerId(player.getId());

        List<RedFlag> toSave = new ArrayList<>();
        int order = 0;
        for (RedFlagInput input : request.flags()) {
            toSave.add(RedFlag.forPlayer(player, input.title().strip(), blankToNull(input.body()), order++));
        }
        return describe(redFlags.saveAll(toSave));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private static List<RedFlagView> describe(List<RedFlag> flags) {
        return flags.stream()
                .map(flag -> new RedFlagView(flag.getId(), flag.getTitle(), flag.getBody()))
                .toList();
    }
}
