package com.nemal.dto;

// CreateCandidateDto.java
public record CreateCandidateDto(
        String name,
        String email,
        String phone,
        Long departmentId,
        Long targetDesignationId,
        String resumeUrl,
        String notes,
        Integer yearsOfExperience
) {}