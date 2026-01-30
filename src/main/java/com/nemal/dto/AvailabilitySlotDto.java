package com.nemal.dto;

import com.nemal.entity.AvailabilitySlot;
import com.nemal.enums.SlotStatus;

import java.time.LocalDateTime;

public record AvailabilitySlotDto(
        Long id,
        Long interviewerId,
        String interviewerName,
        LocalDateTime startDateTime,
        LocalDateTime endDateTime,
        SlotStatus status,
        String description,
        Long interviewScheduleId,
        boolean isActive
) {
    public static AvailabilitySlotDto from(AvailabilitySlot slot) {
        return new AvailabilitySlotDto(
                slot.getId(),
                slot.getInterviewer().getId(),
                slot.getInterviewer().getFullName(),
                slot.getStartDateTime(),
                slot.getEndDateTime(),
                slot.getStatus(),
                slot.getDescription(),
                slot.getInterviewSchedule() != null ? slot.getInterviewSchedule().getId() : null,
                slot.isActive()
        );
    }
}