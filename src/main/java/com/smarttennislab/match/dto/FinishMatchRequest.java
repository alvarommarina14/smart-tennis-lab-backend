package com.smarttennislab.match.dto;

import com.smarttennislab.match.model.MatchStatus;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record FinishMatchRequest(
        @NotNull(message = "Falta el estado final") MatchStatus status,
        @NotNull(message = "Falta la hora de fin") Instant finishedAt,
        String notes) {}
