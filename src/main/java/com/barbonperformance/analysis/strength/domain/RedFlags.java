package com.barbonperformance.analysis.strength.domain;

import java.util.List;
import java.util.UUID;

public interface RedFlags {

    List<RedFlag> findLibrary(UUID orgId);

    List<RedFlag> findAllByPlayerId(UUID playerProfileId);

    void deleteLibrary(UUID orgId);

    void deleteAllByPlayerId(UUID playerProfileId);

    List<RedFlag> saveAll(List<RedFlag> redFlags);
}
