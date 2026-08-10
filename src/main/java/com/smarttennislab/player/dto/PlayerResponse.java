package com.smarttennislab.player.dto;

import com.smarttennislab.player.model.DominantHand;
import com.smarttennislab.player.model.Player;
import java.time.LocalDate;
import java.util.UUID;

public record PlayerResponse(
        UUID id,
        String firstName,
        String lastName,
        LocalDate birthDate,
        DominantHand dominantHand,
        String notes,
        boolean archived) {

    public static PlayerResponse from(Player player) {
        return new PlayerResponse(
                player.getId(),
                player.getFirstName(),
                player.getLastName(),
                player.getBirthDate(),
                player.getDominantHand(),
                player.getNotes(),
                player.isArchived());
    }
}
