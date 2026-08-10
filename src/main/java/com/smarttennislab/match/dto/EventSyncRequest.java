package com.smarttennislab.match.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record EventSyncRequest(@NotEmpty(message = "El lote viene vacío") @Valid List<Event> events) {

    // deleted = true es el undo. No borra la fila: la marca, y por eso reenviar el lote da siempre
    // el mismo resultado.
    public record Event(
            @NotNull(message = "Falta el id del evento") UUID id,
            UUID setId,
            @NotNull(message = "Falta el KPI") String kpiCode,
            @NotNull(message = "Falta cuándo ocurrió") Instant occurredAt,
            @PositiveOrZero(message = "El momento del video no puede ser negativo") Long videoOffsetMs,
            long clientSeq,
            boolean deleted) {}
}
