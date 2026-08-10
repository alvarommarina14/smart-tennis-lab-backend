package com.smarttennislab.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "Poné tu contraseña actual") String currentPassword,
        @NotBlank(message = "La contraseña nueva es obligatoria")
                @Size(min = 8, max = 72, message = "La contraseña tiene que tener al menos 8 caracteres")
                String newPassword) {}
