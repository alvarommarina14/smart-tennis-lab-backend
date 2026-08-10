package com.smarttennislab.match.dto;

import com.smarttennislab.catalog.model.Discipline;
import com.smarttennislab.match.model.Surface;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

// El id viene del celular: es lo que permite crear el partido sin señal y sincronizarlo después
// sin duplicarlo.
public record CreateMatchRequest(
        @NotNull(message = "Falta el id del partido") UUID id,
        @NotNull(message = "Hay que elegir un alumno") UUID playerId,
        @Size(max = 120) String opponentName,
        @Size(max = 120) String tournament,
        Surface surface,
        Discipline discipline,
        @NotNull(message = "Falta la hora de inicio") Instant startedAt,
        String notes) {}
