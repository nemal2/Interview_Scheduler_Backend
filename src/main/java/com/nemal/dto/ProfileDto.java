package com.nemal.dto;

import com.nemal.entity.User;

public record ProfileDto(
        Long id,
        String email,
        String firstName,
        String lastName,
        String phone,
        String profilePictureUrl,
        String role,
        DepartmentSimpleDto department,
        DesignationSimpleDto currentDesignation,
        Integer yearsOfExperience,
        String bio
) {
    public static ProfileDto from(User user) {
        return new ProfileDto(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhone(),
                user.getProfilePictureUrl(),
                user.getRole().name(),
                user.getDepartment() != null ? DepartmentSimpleDto.from(user.getDepartment()) : null,
                user.getCurrentDesignation() != null ? DesignationSimpleDto.from(user.getCurrentDesignation()) : null,
                user.getYearsOfExperience() != null ? user.getYearsOfExperience() : 0,
                user.getBio()
        );
    }
}