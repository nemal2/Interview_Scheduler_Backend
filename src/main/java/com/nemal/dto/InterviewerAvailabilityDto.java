package com.nemal.dto;

import com.nemal.entity.AvailabilitySlot;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public record InterviewerAvailabilityDto(
        Long slotId,
        Long interviewerId,
        String interviewerName,
        String interviewerEmail,
        String department,
        String designation,
        List<String> technologies,
        LocalDateTime startDateTime,
        LocalDateTime endDateTime,
        String status,
        Integer yearsOfExperience,
        String candidateName   // populated for BOOKED slots
) {
    public static InterviewerAvailabilityDto from(AvailabilitySlot slot) {
        if (slot == null || slot.getInterviewer() == null) {
            throw new IllegalArgumentException("AvailabilitySlot or Interviewer cannot be null");
        }

        var interviewer = slot.getInterviewer();

        String departmentName = interviewer.getDepartment() != null
                ? interviewer.getDepartment().getName() : null;

        String designationName = interviewer.getCurrentDesignation() != null
                ? interviewer.getCurrentDesignation().getName() : null;

        List<String> techList = List.of();
        try {
            if (interviewer.getInterviewerTechnologies() != null) {
                techList = interviewer.getInterviewerTechnologies().stream()
                        .filter(it -> it != null && it.isActive() && it.getTechnology() != null)
                        .map(it -> it.getTechnology().getName())
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            System.err.println("Error getting technologies for interviewer "
                    + interviewer.getId() + ": " + e.getMessage());
        }

        // Resolve candidateName for booked slots
        String candidateName = null;
        if ("BOOKED".equals(slot.getStatus().name())) {
            // Via interviewSchedule → request chain
            if (slot.getInterviewSchedule() != null
                    && slot.getInterviewSchedule().getRequest() != null) {
                candidateName = slot.getInterviewSchedule().getRequest().getCandidateName();
            }
            // Fall back to description pattern
            if (candidateName == null && slot.getDescription() != null) {
                String desc = slot.getDescription();
                if (desc.startsWith("Panel Interview: ")) {
                    candidateName = desc.substring("Panel Interview: ".length()).trim();
                } else if (desc.startsWith("Interview: ")) {
                    candidateName = desc.substring("Interview: ".length()).trim();
                }
            }
        }

        return new InterviewerAvailabilityDto(
                slot.getId(),
                interviewer.getId(),
                interviewer.getFirstName() + " " + interviewer.getLastName(),
                interviewer.getEmail(),
                departmentName,
                designationName,
                techList,
                slot.getStartDateTime(),
                slot.getEndDateTime(),
                slot.getStatus().name(),
                interviewer.getYearsOfExperience(),
                candidateName
        );
    }
}