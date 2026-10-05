package com.barbonperformance.analysis.strength.application;

import com.barbonperformance.analysis.strength.application.dto.PlanDtos.PlanView;
import com.barbonperformance.analysis.strength.application.dto.PlanDtos.SetPlanStartRequest;
import com.barbonperformance.analysis.user.domain.PlanCalendar;
import com.barbonperformance.analysis.user.domain.PlayerProfile;
import com.barbonperformance.analysis.user.domain.PlayerProfiles;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlanService {

    private final PlayerAccess access;
    private final PlayerProfiles playerProfiles;

    public PlanService(PlayerAccess access, PlayerProfiles playerProfiles) {
        this.access = access;
        this.playerProfiles = playerProfiles;
    }

    @Transactional(readOnly = true)
    public PlanView get(UUID userId, UUID playerProfileId) {
        return describe(access.readableBy(userId, playerProfileId));
    }

    @Transactional
    public PlanView setStart(UUID coachUserId, UUID playerProfileId, SetPlanStartRequest request) {
        PlayerProfile player = access.ownedByCoach(coachUserId, playerProfileId);
        player.setPlanStartedAt(request.startDate());
        return describe(playerProfiles.save(player));
    }

    static PlanView describe(PlayerProfile player) {
        Integer planWeek = PlanCalendar.planWeek(player).orElse(null);
        return new PlanView(
                player.getPlanStartedAt(),
                planWeek,
                PlanCalendar.PLAN_WEEKS,
                planWeek == null ? null : PlanCalendar.cycleWeek(planWeek),
                PlanCalendar.unlockedWeeks(player));
    }
}
