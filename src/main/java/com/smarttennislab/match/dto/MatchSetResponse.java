package com.smarttennislab.match.dto;

import com.smarttennislab.match.model.MatchSet;
import java.time.Instant;
import java.util.UUID;

public record MatchSetResponse(UUID id, short setNumber, Instant startedAt, Instant finishedAt) {

    public static MatchSetResponse from(MatchSet set) {
        return new MatchSetResponse(
                set.getId(), set.getSetNumber(), set.getStartedAt(), set.getFinishedAt());
    }
}
