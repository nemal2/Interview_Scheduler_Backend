package com.nemal.dto;

import com.nemal.enums.CandidateStatus;

public record UpdateCandidateDto(
        String name,
        String email,
        String phone,
        Long departmentId,
        Long targetDesignationId,
        CandidateStatus status,
        String resumeUrl,
        String notes,
        Integer yearsOfExperience,
        Boolean isActive
) {}