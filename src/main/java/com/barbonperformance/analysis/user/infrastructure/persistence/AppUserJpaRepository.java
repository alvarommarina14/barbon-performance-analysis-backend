package com.barbonperformance.analysis.user.infrastructure.persistence;

import com.barbonperformance.analysis.user.domain.AppUser;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface AppUserJpaRepository extends JpaRepository<AppUser, UUID> {

    Optional<AppUser> findByOrganizationSlugAndEmail(String organizationSlug, String email);

    boolean existsByOrganizationSlugAndEmail(String organizationSlug, String email);
}
