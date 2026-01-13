package com.nemal.dto;

public record ProfileUpdateDto(
        String firstName,
        String lastName,
        String phone,
        String profilePictureUrl,
        Long departmentId,
        Long designationId,
        String bio,
        Integer yearsOfExperience
) {}