package com.smarttennislab.report.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SetReportResponse(
        UUID setId,
        short setNumber,
        Instant startedAt,
        Instant finishedAt,
        List<KpiValueResponse> kpis) {}
