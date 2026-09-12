package com.barbonperformance.analysis.drills.infrastructure.persistence;

import com.barbonperformance.analysis.drills.domain.LibraryItem;
import com.barbonperformance.analysis.drills.domain.LibraryItems;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
class JpaLibraryItems implements LibraryItems {

    private final LibraryItemJpaRepository jpa;

    JpaLibraryItems(LibraryItemJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public List<LibraryItem> findAllByOrgId(UUID orgId) {
        return jpa.findAllByOrganizationId(orgId);
    }

    @Override
    public Optional<LibraryItem> findById(UUID id) {
        return jpa.findById(id);
    }

    @Override
    public LibraryItem save(LibraryItem item) {
        return jpa.save(item);
    }

    @Override
    public void delete(LibraryItem item) {
        jpa.delete(item);
    }
}
