package com.smarttennislab.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(@NotBlank(message = "Falta el refresh token") String refreshToken) {}
