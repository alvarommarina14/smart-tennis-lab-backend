package com.smarttennislab.match.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record StartSetRequest(
        @NotNull(message = "Falta el id del set") UUID id,
        @Min(value = 1, message = "El set va del 1 al 5")
                @Max(value = 5, message = "El set va del 1 al 5")
                short setNumber,
        @NotNull(message = "Falta la hora de inicio") Instant startedAt) {}
