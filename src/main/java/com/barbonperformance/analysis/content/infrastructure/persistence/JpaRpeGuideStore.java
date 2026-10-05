package com.barbonperformance.analysis.content.infrastructure.persistence;

import com.barbonperformance.analysis.content.domain.RpeGuide;
import com.barbonperformance.analysis.content.domain.RpeGuideStore;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
class JpaRpeGuideStore implements RpeGuideStore {

    private final RpeGuideJpaRepository jpa;

    JpaRpeGuideStore(RpeGuideJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<RpeGuide> findByOrgId(UUID orgId) {
        return jpa.findByOrganizationId(orgId);
    }

    @Override
    public RpeGuide save(RpeGuide guide) {
        return jpa.save(guide);
    }
}
