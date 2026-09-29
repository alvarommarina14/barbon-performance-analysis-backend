package com.barbonperformance.analysis.strength.infrastructure.persistence;

import com.barbonperformance.analysis.strength.domain.RedFlag;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

interface RedFlagJpaRepository extends JpaRepository<RedFlag, UUID> {

    List<RedFlag> findAllByOrganizationIdAndPlayerIsNullOrderBySortOrder(UUID orgId);

    List<RedFlag> findAllByPlayerIdOrderBySortOrder(UUID playerProfileId);

    @Modifying
    @Query("delete from RedFlag f where f.organization.id = :orgId and f.player is null")
    void deleteAllForEveryone(UUID orgId);

    @Modifying
    @Query("delete from RedFlag f where f.player.id = :playerProfileId")
    void deleteAllByPlayerId(UUID playerProfileId);
}
