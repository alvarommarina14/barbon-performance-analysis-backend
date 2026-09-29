package com.barbonperformance.analysis.content.application;

import com.barbonperformance.analysis.content.ContentProperties;
import com.barbonperformance.analysis.content.application.dto.ContentDtos.ConfirmPdfUploadRequest;
import com.barbonperformance.analysis.content.application.dto.ContentDtos.PdfUploadIntentRequest;
import com.barbonperformance.analysis.content.application.dto.ContentDtos.PdfUploadIntentResponse;
import com.barbonperformance.analysis.content.application.dto.ContentDtos.RpeGuideView;
import com.barbonperformance.analysis.content.domain.ObjectStorage;
import com.barbonperformance.analysis.content.domain.RpeGuide;
import com.barbonperformance.analysis.content.domain.RpeGuideStore;
import com.barbonperformance.analysis.organization.domain.Organization;
import com.barbonperformance.analysis.shared.domain.NotFoundException;
import com.barbonperformance.analysis.user.domain.AppUsers;
import com.barbonperformance.analysis.user.domain.CoachProfile;
import com.barbonperformance.analysis.user.domain.CoachProfiles;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RpeGuideService {

    private static final String PDF_KEY_TEMPLATE = "content/%s/rpe-guide/guide.pdf";

    private final AppUsers users;
    private final CoachProfiles coachProfiles;
    private final RpeGuideStore store;
    private final ObjectStorage objectStorage;
    private final ContentProperties contentProperties;

    public RpeGuideService(AppUsers users, CoachProfiles coachProfiles, RpeGuideStore store,
            ObjectStorage objectStorage, ContentProperties contentProperties) {
        this.users = users;
        this.coachProfiles = coachProfiles;
        this.store = store;
        this.objectStorage = objectStorage;
        this.contentProperties = contentProperties;
    }

    // Nothing uploaded yet is a valid state (a brand new org), not a 404.
    @Transactional(readOnly = true)
    public RpeGuideView get(UUID userId) {
        UUID orgId = users.findById(userId)
                .orElseThrow(() -> new NotFoundException("User " + userId + " not found"))
                .getOrganization().getId();
        return store.findByOrgId(orgId).map(this::describe)
                .orElseGet(() -> new RpeGuideView(null, null, null, null, null));
    }

    @Transactional
    public PdfUploadIntentResponse requestPdfUploadIntent(UUID coachUserId,
            PdfUploadIntentRequest request) {
        CoachProfile coach = coach(coachUserId);
        String key = PDF_KEY_TEMPLATE.formatted(coach.getOrganization().getId());
        ObjectStorage.PresignedUpload upload =
                objectStorage.presignUpload(key, contentProperties.presignTtl(), request.contentType());
        return new PdfUploadIntentResponse(upload.uploadUrl(), upload.objectKey());
    }

    @Transactional
    public RpeGuideView confirmPdf(UUID coachUserId, ConfirmPdfUploadRequest request) {
        CoachProfile coach = coach(coachUserId);
        UploadedObjects.requireKey(request.objectKey(), PDF_KEY_TEMPLATE.formatted(coach.getOrganization().getId()));
        RpeGuide guide = guideFor(coach.getOrganization());
        guide.attachPdf(request.objectKey(), request.contentType(), request.fileName(),
                request.sizeBytes(), coach.getUser());
        return describe(store.save(guide));
    }

    private RpeGuide guideFor(Organization organization) {
        return store.findByOrgId(organization.getId())
                .orElseGet(() -> new RpeGuide(organization));
    }

    private RpeGuideView describe(RpeGuide guide) {
        String pdfUrl = guide.getPdfObjectKey() == null ? null
                : objectStorage.presignDownload(guide.getPdfObjectKey(), contentProperties.presignTtl());
        return new RpeGuideView(
                pdfUrl, guide.getPdfFileName(), guide.getPdfSizeBytes(), guide.getPdfUploadedAt(),
                guide.getUpdatedAt());
    }

    private CoachProfile coach(UUID coachUserId) {
        return coachProfiles.findByUserId(coachUserId)
                .orElseThrow(() -> new NotFoundException("No coach profile for this account"));
    }
}
