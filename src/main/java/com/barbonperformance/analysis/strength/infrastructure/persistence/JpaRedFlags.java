package com.barbonperformance.analysis.strength.infrastructure.persistence;

import com.barbonperformance.analysis.strength.domain.RedFlag;
import com.barbonperformance.analysis.strength.domain.RedFlags;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
class JpaRedFlags implements RedFlags {

    private final RedFlagJpaRepository jpa;

    JpaRedFlags(RedFlagJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public List<RedFlag> findLibrary(UUID orgId) {
        return jpa.findAllByOrganizationIdAndPlayerIsNullOrderBySortOrder(orgId);
    }

    @Override
    public List<RedFlag> findAllByPlayerId(UUID playerProfileId) {
        return jpa.findAllByPlayerIdOrderBySortOrder(playerProfileId);
    }

    @Override
    public void deleteLibrary(UUID orgId) {
        jpa.deleteLibrary(orgId);
    }

    @Override
    public void deleteAllByPlayerId(UUID playerProfileId) {
        jpa.deleteAllByPlayerId(playerProfileId);
    }

    @Override
    public List<RedFlag> saveAll(List<RedFlag> redFlags) {
        return jpa.saveAll(redFlags);
    }
}
