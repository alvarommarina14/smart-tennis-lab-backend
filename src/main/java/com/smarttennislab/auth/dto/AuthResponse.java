package com.smarttennislab.auth.dto;

public record AuthResponse(
        String accessToken, String refreshToken, long expiresInSeconds, CoachResponse coach) {}
