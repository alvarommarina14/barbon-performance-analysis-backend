package com.barbonperformance.analysis.content.infrastructure.persistence;

import com.barbonperformance.analysis.content.domain.RecordingTips;
import com.barbonperformance.analysis.content.domain.RecordingTipsStore;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
class JpaRecordingTipsStore implements RecordingTipsStore {

    private final RecordingTipsJpaRepository jpa;

    JpaRecordingTipsStore(RecordingTipsJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<RecordingTips> findByOrgId(UUID orgId) {
        return jpa.findByOrganizationId(orgId);
    }

    @Override
    public RecordingTips save(RecordingTips tips) {
        return jpa.save(tips);
    }
}
