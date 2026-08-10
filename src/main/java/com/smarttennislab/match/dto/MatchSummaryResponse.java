package com.smarttennislab.match.dto;

import com.smarttennislab.match.model.Match;
import com.smarttennislab.match.model.MatchStatus;
import java.time.Instant;
import java.util.UUID;

public record MatchSummaryResponse(
        UUID id,
        UUID playerId,
        String playerName,
        String opponentName,
        MatchStatus status,
        Instant startedAt,
        Instant finishedAt) {

    public static MatchSummaryResponse from(Match match, String playerName) {
        return new MatchSummaryResponse(
                match.getId(),
                match.getPlayerId(),
                playerName,
                match.getOpponentName(),
                match.getStatus(),
                match.getStartedAt(),
                match.getFinishedAt());
    }
}
