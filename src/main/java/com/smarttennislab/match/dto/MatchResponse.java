package com.smarttennislab.match.dto;

import com.smarttennislab.catalog.model.Discipline;
import com.smarttennislab.match.model.Match;
import com.smarttennislab.match.model.MatchStatus;
import com.smarttennislab.match.model.Surface;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MatchResponse(
        UUID id,
        UUID playerId,
        String playerName,
        String opponentName,
        String tournament,
        Surface surface,
        Discipline discipline,
        MatchStatus status,
        Instant startedAt,
        Instant finishedAt,
        String notes,
        long eventCount,
        List<MatchSetResponse> sets) {

    public static MatchResponse from(
            Match match, String playerName, long eventCount, List<MatchSetResponse> sets) {
        return new MatchResponse(
                match.getId(),
                match.getPlayerId(),
                playerName,
                match.getOpponentName(),
                match.getTournament(),
                match.getSurface(),
                match.getDiscipline(),
                match.getStatus(),
                match.getStartedAt(),
                match.getFinishedAt(),
                match.getNotes(),
                eventCount,
                sets);
    }
}
