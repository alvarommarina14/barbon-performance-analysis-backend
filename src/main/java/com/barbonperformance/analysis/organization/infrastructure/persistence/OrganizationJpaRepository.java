package com.barbonperformance.analysis.organization.infrastructure.persistence;

import com.barbonperformance.analysis.organization.domain.Organization;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface OrganizationJpaRepository extends JpaRepository<Organization, UUID> {

    Optional<Organization> findBySlug(String slug);
}
