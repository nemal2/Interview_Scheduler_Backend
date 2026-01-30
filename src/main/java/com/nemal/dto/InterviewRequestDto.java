// InterviewRequestDto.java
package com.nemal.dto;

import com.nemal.entity.InterviewRequest;
import com.nemal.enums.RequestStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public record InterviewRequestDto(
        Long id,
        String candidateName,
        String candidateDesignation,
        List<String> requiredTechnologies,
        LocalDateTime preferredStartDateTime,
        LocalDateTime preferredEndDateTime,
        String requestedByName,
        String assignedInterviewerName,
        Long assignedInterviewerId,
        Long availabilitySlotId,
        RequestStatus status,
        boolean isUrgent,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime respondedAt
) {
    public static InterviewRequestDto from(InterviewRequest request) {
        return new InterviewRequestDto(
                request.getId(),
                request.getCandidateName(),
                request.getCandidateDesignation() != null ? request.getCandidateDesignation().getName() : null,
                request.getRequiredTechnologies().stream()
                        .map(tech -> tech.getName())
                        .collect(Collectors.toList()),
                request.getPreferredStartDateTime(),
                request.getPreferredEndDateTime(),
                request.getRequestedBy() != null ? request.getRequestedBy().getFullName() : null,
                request.getAssignedInterviewer() != null ? request.getAssignedInterviewer().getFullName() : null,
                request.getAssignedInterviewer() != null ? request.getAssignedInterviewer().getId() : null,
                request.getAvailabilitySlot() != null ? request.getAvailabilitySlot().getId() : null,
                request.getStatus(),
                request.isUrgent(),
                request.getNotes(),
                request.getCreatedAt(),
                request.getRespondedAt()
        );
    }
}