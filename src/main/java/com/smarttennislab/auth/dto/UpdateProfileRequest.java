package com.smarttennislab.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank(message = "El email es obligatorio")
                @Email(message = "El email no es válido")
                @Size(max = 255)
                String email,
        @NotBlank(message = "El nombre es obligatorio") @Size(max = 120) String fullName) {}
