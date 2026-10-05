package com.barbonperformance.analysis.strength.application;

import com.barbonperformance.analysis.organization.domain.Organization;
import com.barbonperformance.analysis.strength.application.dto.RedFlagDtos.RedFlagInput;
import com.barbonperformance.analysis.strength.application.dto.RedFlagDtos.RedFlagView;
import com.barbonperformance.analysis.strength.application.dto.RedFlagDtos.ReplaceRedFlagsRequest;
import com.barbonperformance.analysis.strength.domain.RedFlag;
import com.barbonperformance.analysis.strength.domain.RedFlags;
import com.barbonperformance.analysis.user.domain.PlayerProfile;
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

    @Transactional(readOnly = true)
    public List<RedFlagView> library(UUID coachUserId) {
        return describe(redFlags.findLibrary(access.coach(coachUserId).getOrganization().getId()));
    }

    // Picking a library entry copies it into the player's list, like applying a cycle phase
    // template, so editing the library never changes what a player already has.
    @Transactional
    public List<RedFlagView> replaceLibrary(UUID coachUserId, ReplaceRedFlagsRequest request) {
        Organization organization = access.coach(coachUserId).getOrganization();
        redFlags.deleteLibrary(organization.getId());

        List<RedFlag> toSave = new ArrayList<>();
        int order = 0;
        for (RedFlagInput input : request.flags()) {
            toSave.add(RedFlag.inLibrary(organization, input.title().strip(), blankToNull(input.body()), order++));
        }
        return describe(redFlags.saveAll(toSave));
    }

    @Transactional(readOnly = true)
    public List<RedFlagView> forPlayer(UUID userId, UUID playerProfileId) {
        PlayerProfile player = access.readableBy(userId, playerProfileId);
        return describe(redFlags.findAllByPlayerId(player.getId()));
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
