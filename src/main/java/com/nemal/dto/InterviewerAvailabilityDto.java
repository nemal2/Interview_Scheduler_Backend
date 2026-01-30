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
        Integer yearsOfExperience
) {
    public static InterviewerAvailabilityDto from(AvailabilitySlot slot) {
        if (slot == null || slot.getInterviewer() == null) {
            throw new IllegalArgumentException("AvailabilitySlot or Interviewer cannot be null");
        }

        var interviewer = slot.getInterviewer();

        // Safely get department name
        String departmentName = null;
        if (interviewer.getDepartment() != null) {
            departmentName = interviewer.getDepartment().getName();
        }

        // Safely get designation name
        String designationName = null;
        if (interviewer.getCurrentDesignation() != null) {
            designationName = interviewer.getCurrentDesignation().getName();
        }

        // Safely get technologies
        List<String> techList = List.of();
        try {
            if (interviewer.getInterviewerTechnologies() != null) {
                techList = interviewer.getInterviewerTechnologies().stream()
                        .filter(it -> it != null && it.isActive() && it.getTechnology() != null)
                        .map(it -> it.getTechnology().getName())
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            // Log but don't fail - just return empty list
            System.err.println("Error getting technologies for interviewer " + interviewer.getId() + ": " + e.getMessage());
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
                interviewer.getYearsOfExperience()
        );
    }
}