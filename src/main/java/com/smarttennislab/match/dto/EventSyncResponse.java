package com.smarttennislab.match.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

// serverTime es la marca que la app guarda para el próximo pull incremental.
public record EventSyncResponse(
        int accepted, int ignored, List<Rejected> rejected, long liveEvents, Instant serverTime) {

    public record Rejected(UUID id, String reason) {}
}
