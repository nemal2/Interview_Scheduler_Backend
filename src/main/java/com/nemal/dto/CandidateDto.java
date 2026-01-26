package com.nemal.dto;

import com.nemal.entity.Candidate;
import com.nemal.enums.CandidateStatus;

import java.time.LocalDateTime;

// CandidateDto.java
public record CandidateDto(
        Long id,
        String name,
        String email,
        String phone,
        Long departmentId,
        String departmentName,
        Long targetDesignationId,
        String targetDesignationName,
        String tierName,
        Integer levelOrder,
        CandidateStatus status,
        String resumeUrl,
        String notes,
        Integer yearsOfExperience,
        LocalDateTime appliedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        boolean isActive
) {
    public static CandidateDto from(Candidate candidate) {
        return new CandidateDto(
                candidate.getId(),
                candidate.getName(),
                candidate.getEmail(),
                candidate.getPhone(),
                candidate.getDepartment() != null ? candidate.getDepartment().getId() : null,
                candidate.getDepartment() != null ? candidate.getDepartment().getName() : null,
                candidate.getTargetDesignation() != null ? candidate.getTargetDesignation().getId() : null,
                candidate.getTargetDesignation() != null ? candidate.getTargetDesignation().getName() : null,
                candidate.getTargetDesignation() != null && candidate.getTargetDesignation().getTier() != null
                        ? candidate.getTargetDesignation().getTier().getName() : null,
                candidate.getTargetDesignation() != null ? candidate.getTargetDesignation().getLevelOrder() : null,
                candidate.getStatus(),
                candidate.getResumeUrl(),
                candidate.getNotes(),
                candidate.getYearsOfExperience(),
                candidate.getAppliedAt(),
                candidate.getCreatedAt(),
                candidate.getUpdatedAt(),
                candidate.isActive()
        );
    }
}