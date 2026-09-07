package com.barbonperformance.analysis.analysis.application;

import com.barbonperformance.analysis.analysis.application.dto.AnalysisDtos.AnalysisDetail;
import com.barbonperformance.analysis.analysis.application.dto.AnalysisDtos.AnalysisSummary;
import com.barbonperformance.analysis.analysis.domain.Analyses;
import com.barbonperformance.analysis.analysis.domain.Analysis;
import com.barbonperformance.analysis.analysis.domain.AnalysisStatus;
import com.barbonperformance.analysis.shared.domain.NotFoundException;
import com.barbonperformance.analysis.user.domain.AppUsers;
import com.barbonperformance.analysis.user.domain.CoachProfiles;
import com.barbonperformance.analysis.user.domain.PlayerProfiles;
import com.barbonperformance.analysis.user.domain.UserRole;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalysisReadService {

    private final Analyses analyses;
    private final AppUsers users;
    private final PlayerProfiles playerProfiles;
    private final CoachProfiles coachProfiles;

    public AnalysisReadService(Analyses analyses, AppUsers users, PlayerProfiles playerProfiles,
            CoachProfiles coachProfiles) {
        this.analyses = analyses;
        this.users = users;
        this.playerProfiles = playerProfiles;
        this.coachProfiles = coachProfiles;
    }

    @Transactional(readOnly = true)
    public List<AnalysisSummary> list(UUID userId, UUID playerProfileId) {
        if (role(userId) == UserRole.PLAYER) {
            UUID profileId = playerProfileId(userId);
            return analyses.findAllByPlayerIdAndStatus(profileId, AnalysisStatus.PUBLISHED).stream()
                    .map(AnalysisMapper::summary)
                    .toList();
        }

        UUID coachProfileId = coachProfileId(userId);
        List<Analysis> found = playerProfileId == null
                ? analyses.findAllByCoachId(coachProfileId)
                : analyses.findAllByCoachIdAndPlayerId(coachProfileId, playerProfileId);
        return found.stream().map(AnalysisMapper::summary).toList();
    }

    @Transactional(readOnly = true)
    public AnalysisDetail detail(UUID userId, UUID analysisId) {
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
                // An admin may read any analysis.
            }
        }
        return AnalysisMapper.detail(analysis);
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
        return coachProfiles.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("No coach profile for this account"))
                .getId();
    }
}
