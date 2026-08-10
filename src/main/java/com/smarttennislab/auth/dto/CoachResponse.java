package com.smarttennislab.auth.dto;

import com.smarttennislab.auth.model.User;
import java.util.UUID;

public record CoachResponse(UUID id, String email, String fullName) {

    public static CoachResponse from(User user) {
        return new CoachResponse(user.getId(), user.getEmail(), user.getFullName());
    }
}
