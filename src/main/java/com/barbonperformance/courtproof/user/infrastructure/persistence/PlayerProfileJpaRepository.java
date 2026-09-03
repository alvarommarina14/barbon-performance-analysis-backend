package com.barbonperformance.courtproof.user.infrastructure.persistence;

import com.barbonperformance.courtproof.user.domain.PlayerProfile;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface PlayerProfileJpaRepository extends JpaRepository<PlayerProfile, UUID> {

    @Query("select p from PlayerProfile p join fetch p.user where p.user.id = :userId")
    Optional<PlayerProfile> findByUserId(UUID userId);

    // The coach's client list. Fetches the user eagerly — every row renders a name.
    @Query("""
            select p from PlayerProfile p
            join fetch p.user u
            where p.coach.id = :coachProfileId
            order by u.name
            """)
    List<PlayerProfile> findAllByCoachId(UUID coachProfileId);
}
