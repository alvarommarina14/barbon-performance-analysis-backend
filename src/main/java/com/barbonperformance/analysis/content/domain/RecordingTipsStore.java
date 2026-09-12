package com.barbonperformance.analysis.content.domain;

import java.util.Optional;
import java.util.UUID;

public interface RecordingTipsStore {

    Optional<RecordingTips> findByOrgId(UUID orgId);

    RecordingTips save(RecordingTips tips);
}
