package com.barbonperformance.analysis.drills.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LibraryItems {

    List<LibraryItem> findAllByOrgId(UUID orgId);

    Optional<LibraryItem> findById(UUID id);

    LibraryItem save(LibraryItem item);

    void delete(LibraryItem item);
}
