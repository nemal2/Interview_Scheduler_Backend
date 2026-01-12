package com.nemal.dto;

import com.nemal.entity.User;
import com.nemal.enums.Role;

public record LoginResponse(
        String token,
        Long id,
        String email,
        String firstName,
        String lastName,
        Role role,
        String profilePictureUrl
) {
    public static LoginResponse from(String token, User user) {
        return new LoginResponse(
                token,
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole(),
                user.getProfilePictureUrl()
        );
    }
}