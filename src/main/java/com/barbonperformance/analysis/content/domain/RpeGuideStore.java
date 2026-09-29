package com.barbonperformance.analysis.content.domain;

import java.util.Optional;
import java.util.UUID;

public interface RpeGuideStore {

    Optional<RpeGuide> findByOrgId(UUID orgId);

    RpeGuide save(RpeGuide guide);
}
