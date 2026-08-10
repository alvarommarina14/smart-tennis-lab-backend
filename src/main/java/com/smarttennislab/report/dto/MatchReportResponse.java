package com.smarttennislab.report.dto;

import com.smarttennislab.match.model.MatchStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MatchReportResponse(
        UUID matchId,
        String playerName,
        String opponentName,
        String tournament,
        MatchStatus status,
        Instant startedAt,
        Instant finishedAt,
        long durationMinutes,
        long totalEvents,
        List<ReportCategoryResponse> categories,
        List<SetReportResponse> sets) {}
