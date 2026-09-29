package com.barbonperformance.analysis.player.application;

import com.barbonperformance.analysis.content.ContentProperties;
import com.barbonperformance.analysis.content.domain.ObjectStorage;
import com.barbonperformance.analysis.player.application.dto.ProgramPdfDtos.ConfirmProgramPdfRequest;
import com.barbonperformance.analysis.player.application.dto.ProgramPdfDtos.ProgramPdfUploadIntentRequest;
import com.barbonperformance.analysis.player.application.dto.ProgramPdfDtos.ProgramPdfUploadIntentResponse;
import com.barbonperformance.analysis.shared.domain.NotFoundException;
import com.barbonperformance.analysis.user.domain.CoachProfile;
import com.barbonperformance.analysis.user.domain.CoachProfiles;
import com.barbonperformance.analysis.user.domain.PlayerProfile;
import com.barbonperformance.analysis.user.domain.PlayerProfiles;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProgramPdfService {

    private static final String KEY_TEMPLATE = "content/%s/players/%s/program.pdf";
    private static final String CONTENT_TYPE = "application/pdf";

    private final PlayerProfiles playerProfiles;
    private final CoachProfiles coachProfiles;
    private final ObjectStorage objectStorage;
    private final ContentProperties contentProperties;

    public ProgramPdfService(PlayerProfiles playerProfiles, CoachProfiles coachProfiles,
            ObjectStorage objectStorage, ContentProperties contentProperties) {
        this.playerProfiles = playerProfiles;
        this.coachProfiles = coachProfiles;
        this.objectStorage = objectStorage;
        this.contentProperties = contentProperties;
    }

    @Transactional
    public ProgramPdfUploadIntentResponse requestUploadIntent(UUID coachUserId, UUID playerProfileId,
            ProgramPdfUploadIntentRequest request) {
        PlayerProfile player = loadOwnedPlayer(coachUserId, playerProfileId);
        String key = KEY_TEMPLATE.formatted(player.getOrganization().getId(), player.getId());
        ObjectStorage.PresignedUpload upload =
                objectStorage.presignUpload(key, contentProperties.presignTtl(), CONTENT_TYPE);
        return new ProgramPdfUploadIntentResponse(upload.uploadUrl(), upload.objectKey());
    }

    @Transactional
    public void confirm(UUID coachUserId, UUID playerProfileId, ConfirmProgramPdfRequest request) {
        PlayerProfile player = loadOwnedPlayer(coachUserId, playerProfileId);
        player.setProgramPdfKey(request.objectKey());
        playerProfiles.save(player);
    }

    private PlayerProfile loadOwnedPlayer(UUID coachUserId, UUID playerProfileId) {
        CoachProfile coach = coachProfiles.findByUserId(coachUserId)
                .orElseThrow(() -> new NotFoundException("No coach profile for this account"));
        PlayerProfile player = playerProfiles.findById(playerProfileId)
                .orElseThrow(() -> new NotFoundException("Player " + playerProfileId + " not found"));
        if (player.getCoach() == null || !player.getCoach().getId().equals(coach.getId())) {
            throw new NotFoundException("Player " + playerProfileId + " not found");
        }
        return player;
    }
}
