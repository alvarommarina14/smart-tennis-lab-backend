package com.smarttennislab.match.dto;

import com.smarttennislab.match.model.MatchEvent;
import java.time.Instant;
import java.util.UUID;

public record MatchEventResponse(
        UUID id,
        UUID setId,
        String kpiCode,
        Instant occurredAt,
        Instant recordedAt,
        long clientSeq,
        boolean deleted) {

    public static MatchEventResponse from(MatchEvent event) {
        return new MatchEventResponse(
                event.getId(),
                event.getSetId(),
                event.getKpiCode(),
                event.getOccurredAt(),
                event.getRecordedAt(),
                event.getClientSeq(),
                event.isDeleted());
    }
}
