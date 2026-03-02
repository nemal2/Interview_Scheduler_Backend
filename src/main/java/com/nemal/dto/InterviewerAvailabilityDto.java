package com.nemal.dto;

import com.nemal.entity.AvailabilitySlot;
import com.nemal.entity.InterviewRequest;
import com.nemal.entity.InterviewerTechnology;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * DTO returned to the HR calendar.
 * Includes requestId so the frontend can cancel a BOOKED slot directly.
 */
public record InterviewerAvailabilityDto(
        Long slotId,
        Long interviewerId,
        String interviewerName,
        String department,
        String designation,
        Integer yearsOfExperience,
        List<String> technologies,
        LocalDateTime startDateTime,
        LocalDateTime endDateTime,
        String status,
        String candidateName,
        Long requestId          // ← NEW: ID of the InterviewRequest that booked this slot
) {

    public static InterviewerAvailabilityDto from(AvailabilitySlot slot) {
        String candidateName = null;
        Long requestId = null;

        // Resolve candidateName + requestId from the linked schedule/request
        if (slot.getInterviewSchedule() != null) {
            InterviewRequest req = slot.getInterviewSchedule().getRequest();
            if (req != null) {
                candidateName = req.getCandidateName();
                requestId = req.getId();
            }
        }

        // Fallback: parse "Interview: <name>" or "Panel Interview: <name>" from description
        if (candidateName == null && slot.getDescription() != null) {
            String desc = slot.getDescription();
            if (desc.startsWith("Interview: ")) {
                candidateName = desc.substring("Interview: ".length());
            } else if (desc.startsWith("Panel Interview: ")) {
                candidateName = desc.substring("Panel Interview: ".length());
            }
        }

        List<String> techs = List.of();
        if (slot.getInterviewer() != null && slot.getInterviewer().getInterviewerTechnologies() != null) {
            techs = slot.getInterviewer().getInterviewerTechnologies().stream()
                    .filter(it -> it != null && it.isActive() && it.getTechnology() != null)
                    .map(it -> it.getTechnology().getName())
                    .collect(Collectors.toList());
        }

        return new InterviewerAvailabilityDto(
                slot.getId(),
                slot.getInterviewer() != null ? slot.getInterviewer().getId() : null,
                slot.getInterviewer() != null ? slot.getInterviewer().getFullName() : "Unknown",
                slot.getInterviewer() != null && slot.getInterviewer().getDepartment() != null
                        ? slot.getInterviewer().getDepartment().getName() : null,
                slot.getInterviewer() != null && slot.getInterviewer().getCurrentDesignation() != null
                        ? slot.getInterviewer().getCurrentDesignation().getName() : null,
                slot.getInterviewer() != null ? slot.getInterviewer().getYearsOfExperience() : null,
                techs,
                slot.getStartDateTime(),
                slot.getEndDateTime(),
                slot.getStatus() != null ? slot.getStatus().name() : "AVAILABLE",
                candidateName,
                requestId
        );
    }
}