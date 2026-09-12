package com.barbonperformance.analysis.drills.application;

import com.barbonperformance.analysis.analysis.domain.Analyses;
import com.barbonperformance.analysis.analysis.domain.Analysis;
import com.barbonperformance.analysis.analysis.domain.AnalysisStatus;
import com.barbonperformance.analysis.drills.application.dto.DrillDtos.AssignDrillInput;
import com.barbonperformance.analysis.drills.application.dto.DrillDtos.DrillAssignmentView;
import com.barbonperformance.analysis.drills.application.dto.DrillDtos.ReplaceAssignmentsRequest;
import com.barbonperformance.analysis.drills.domain.DrillAssignment;
import com.barbonperformance.analysis.drills.domain.DrillAssignments;
import com.barbonperformance.analysis.drills.domain.DrillCategory;
import com.barbonperformance.analysis.drills.domain.LibraryItem;
import com.barbonperformance.analysis.drills.domain.LibraryItems;
import com.barbonperformance.analysis.shared.domain.NotFoundException;
import com.barbonperformance.analysis.user.domain.AppUsers;
import com.barbonperformance.analysis.user.domain.CoachProfile;
import com.barbonperformance.analysis.user.domain.CoachProfiles;
import com.barbonperformance.analysis.user.domain.PlayerProfile;
import com.barbonperformance.analysis.user.domain.PlayerProfiles;
import com.barbonperformance.analysis.user.domain.UserRole;
import com.barbonperformance.analysis.video.domain.VideoAsset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DrillAssignmentService {

    private final DrillAssignments assignments;
    private final LibraryItems libraryItems;
    private final Analyses analyses;
    private final AppUsers users;
    private final PlayerProfiles playerProfiles;
    private final CoachProfiles coachProfiles;

    public DrillAssignmentService(DrillAssignments assignments, LibraryItems libraryItems,
            Analyses analyses, AppUsers users, PlayerProfiles playerProfiles,
            CoachProfiles coachProfiles) {
        this.assignments = assignments;
        this.libraryItems = libraryItems;
        this.analyses = analyses;
        this.users = users;
        this.playerProfiles = playerProfiles;
        this.coachProfiles = coachProfiles;
    }

    @Transactional(readOnly = true)
    public List<DrillAssignmentView> listForCaller(UUID userId, DrillCategory category,
            UUID analysisId, UUID playerProfileId) {
        if (analysisId != null) {
            return forAnalysis(userId, analysisId, category);
        }
        if (playerProfileId != null) {
            return forPlayer(userId, playerProfileId, category);
        }
        return forCallerOwnPlayer(userId, category);
    }

    @Transactional
    public List<DrillAssignmentView> replaceForAnalysis(UUID coachUserId, UUID analysisId,
            ReplaceAssignmentsRequest request) {
        CoachProfile coach = coach(coachUserId);
        Analysis analysis = analyses.findDetailedById(analysisId)
                .orElseThrow(() -> new NotFoundException("Analysis " + analysisId + " not found"));
        if (!analysis.getCoach().getId().equals(coach.getId())) {
            throw new NotFoundException("Analysis " + analysisId + " not found");
        }

        assignments.deleteAllByAnalysisId(analysisId);

        List<DrillAssignment> toSave = new ArrayList<>();
        int order = 0;
        for (AssignDrillInput input : request.drills()) {
            LibraryItem item = libraryItems.findById(input.libraryItemId())
                    .filter(i -> i.getOrganization().getId().equals(coach.getOrganization().getId()))
                    .orElseThrow(() -> new NotFoundException(
                            "Drill " + input.libraryItemId() + " not found"));
            toSave.add(new DrillAssignment(
                    coach.getOrganization(), analysis.getPlayer(), item, analysis,
                    input.coachNote(), order++));
        }

        return assignments.saveAll(toSave).stream()
                .map(DrillAssignmentService::describe)
                .toList();
    }

    // A missing/foreign analysis is reported as missing rather than forbidden, same convention as
    // AnalysisReadService.detail.
    private List<DrillAssignmentView> forAnalysis(UUID userId, UUID analysisId, DrillCategory category) {
        Analysis analysis = analyses.findDetailedById(analysisId)
                .orElseThrow(() -> new NotFoundException("Analysis " + analysisId + " not found"));

        switch (role(userId)) {
            case PLAYER -> {
                boolean ownedAndPublished = analysis.getStatus() == AnalysisStatus.PUBLISHED
                        && analysis.getPlayer().getId().equals(playerProfileId(userId));
                if (!ownedAndPublished) {
                    throw new NotFoundException("Analysis " + analysisId + " not found");
                }
            }
            case COACH -> {
                if (!analysis.getCoach().getId().equals(coachProfileId(userId))) {
                    throw new NotFoundException("Analysis " + analysisId + " not found");
                }
            }
            case ADMIN -> {
                // An admin may read any analysis' drills.
            }
        }

        return filterByCategory(assignments.findAllByAnalysisId(analysisId), category);
    }

    private List<DrillAssignmentView> forPlayer(UUID userId, UUID playerProfileId, DrillCategory category) {
        if (role(userId) == UserRole.PLAYER) {
            throw new AccessDeniedException("Not entitled to another player's drills");
        }
        PlayerProfile player = playerProfiles.findById(playerProfileId)
                .orElseThrow(() -> new NotFoundException("Player " + playerProfileId + " not found"));
        if (role(userId) == UserRole.COACH
                && (player.getCoach() == null
                        || !player.getCoach().getId().equals(coachProfileId(userId)))) {
            throw new NotFoundException("Player " + playerProfileId + " not found");
        }

        return assignments.findAllByPlayerId(playerProfileId, category).stream()
                .map(DrillAssignmentService::describe)
                .toList();
    }

    // No filters: a coach has no single "my drills" list, only a player does.
    private List<DrillAssignmentView> forCallerOwnPlayer(UUID userId, DrillCategory category) {
        if (role(userId) != UserRole.PLAYER) {
            return List.of();
        }
        return assignments.findAllByPlayerId(playerProfileId(userId), category).stream()
                .map(DrillAssignmentService::describe)
                .toList();
    }

    private List<DrillAssignmentView> filterByCategory(List<DrillAssignment> found, DrillCategory category) {
        return found.stream()
                .filter(a -> category == null || a.getLibraryItem().getCategory() == category)
                .map(DrillAssignmentService::describe)
                .toList();
    }

    private UserRole role(UUID userId) {
        return users.findById(userId)
                .orElseThrow(() -> new NotFoundException("User " + userId + " not found"))
                .getRole();
    }

    private UUID playerProfileId(UUID userId) {
        return playerProfiles.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("No player profile for this account"))
                .getId();
    }

    private UUID coachProfileId(UUID userId) {
        return coach(userId).getId();
    }

    private CoachProfile coach(UUID coachUserId) {
        return coachProfiles.findByUserId(coachUserId)
                .orElseThrow(() -> new NotFoundException("No coach profile for this account"));
    }

    static DrillAssignmentView describe(DrillAssignment assignment) {
        LibraryItem item = assignment.getLibraryItem();
        VideoAsset asset = item.getVideoAsset();
        Analysis analysis = assignment.getAnalysis();
        return new DrillAssignmentView(
                assignment.getId(),
                item.getId(),
                item.getTitle(),
                item.getCue(),
                item.getCategory(),
                asset.getId(),
                asset.getStatus(),
                asset.getDurationSeconds(),
                asset.getThumbnailUrl(),
                assignment.getCoachNote(),
                analysis == null ? null : analysis.getId(),
                analysis == null ? null : analysis.getTitle(),
                assignment.getSortOrder());
    }
}
