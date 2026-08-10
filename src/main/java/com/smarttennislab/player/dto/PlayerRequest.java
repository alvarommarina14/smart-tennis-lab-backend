package com.smarttennislab.player.dto;

import com.smarttennislab.player.model.DominantHand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record PlayerRequest(
        @NotBlank(message = "El nombre es obligatorio") @Size(max = 80) String firstName,
        @NotBlank(message = "El apellido es obligatorio") @Size(max = 80) String lastName,
        @Past(message = "La fecha de nacimiento tiene que ser pasada") LocalDate birthDate,
        DominantHand dominantHand,
        String notes) {}
