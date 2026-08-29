package com.barbonperformance.courtproof.user.persistence;

import com.barbonperformance.courtproof.user.domain.AppUser;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, UUID> {

    Optional<AppUser> findByOrganizationSlugAndEmail(String organizationSlug, String email);

    boolean existsByOrganizationSlugAndEmail(String organizationSlug, String email);
}
