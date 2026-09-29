package com.barbonperformance.analysis.strength.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public final class RedFlagDtos {

    private RedFlagDtos() {
    }

    public record RedFlagInput(
            @NotBlank @Size(max = 200) String title,
            @Size(max = 1000) String body) {
    }

    public record ReplaceRedFlagsRequest(@NotNull @Valid @Size(max = 50) List<RedFlagInput> flags) {
    }

    public record RedFlagView(@NotNull UUID id, @NotNull String title, String body) {
    }

    public record RedFlagsView(@NotNull List<RedFlagView> forEveryone, @NotNull List<RedFlagView> forPlayer) {
    }
}
