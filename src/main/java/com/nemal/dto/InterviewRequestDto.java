package com.nemal.dto;

import com.nemal.entity.InterviewRequest;
import com.nemal.enums.RequestStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public record InterviewRequestDto(
        Long id,
        String candidateName,
        Long candidateId,
        Long candidateDesignationId,
        String candidateDesignationName,
        List<TechnologySimpleDto> requiredTechnologies,
        LocalDateTime preferredStartDateTime,
        LocalDateTime preferredEndDateTime,
        Long requestedById,
        String requestedByName,
        Long assignedInterviewerId,
        String assignedInterviewerName,
        Long availabilitySlotId,
        Long panelId,
        RequestStatus status,
        LocalDateTime respondedAt,
        String responseNotes,
        boolean isUrgent,
        String notes,
        LocalDateTime createdAt
) {
    public static InterviewRequestDto from(InterviewRequest request) {
        return new InterviewRequestDto(
                request.getId(),
                request.getCandidateName(),
                request.getCandidate() != null ? request.getCandidate().getId() : null,
                request.getCandidateDesignation() != null ? request.getCandidateDesignation().getId() : null,
                request.getCandidateDesignation() != null ? request.getCandidateDesignation().getName() : null,
                request.getRequiredTechnologies() != null
                        ? request.getRequiredTechnologies().stream()
                        .map(t -> new TechnologySimpleDto(t.getId(), t.getName(), t.getCategory()))
                        .collect(Collectors.toList())
                        : List.of(),
                request.getPreferredStartDateTime(),
                request.getPreferredEndDateTime(),
                request.getRequestedBy() != null ? request.getRequestedBy().getId() : null,
                request.getRequestedBy() != null ? request.getRequestedBy().getFullName() : null,
                request.getAssignedInterviewer() != null ? request.getAssignedInterviewer().getId() : null,
                request.getAssignedInterviewer() != null ? request.getAssignedInterviewer().getFullName() : null,
                request.getAvailabilitySlot() != null ? request.getAvailabilitySlot().getId() : null,
                request.getPanel() != null ? request.getPanel().getId() : null,
                request.getStatus(),
                request.getRespondedAt(),
                request.getResponseNotes(),
                request.isUrgent(),
                request.getNotes(),
                request.getCreatedAt()
        );
    }
}