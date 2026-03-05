package com.nemal.service;

import com.nemal.dto.AvailabilitySlotDto;
import com.nemal.dto.BulkAvailabilitySlotDto;
import com.nemal.dto.CreateAvailabilitySlotDto;
import com.nemal.dto.UpdateAvailabilitySlotDto;
import com.nemal.entity.AvailabilitySlot;
import com.nemal.entity.User;
import com.nemal.enums.SlotStatus;
import com.nemal.repository.AvailabilitySlotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AvailabilityService {

    private final AvailabilitySlotRepository availabilitySlotRepository;

    public AvailabilityService(AvailabilitySlotRepository availabilitySlotRepository) {
        this.availabilitySlotRepository = availabilitySlotRepository;
    }

    public List<AvailabilitySlotDto> getInterviewerAvailability(User interviewer) {
        return availabilitySlotRepository.findByInterviewerIdAndIsActiveTrue(interviewer.getId())
                .stream()
                .map(AvailabilitySlotDto::from)
                .collect(Collectors.toList());
    }

    public List<AvailabilitySlotDto> getInterviewerAvailabilityByDateRange(
            User interviewer,
            LocalDateTime start,
            LocalDateTime end
    ) {
        return availabilitySlotRepository.findByInterviewerIdAndStartDateTimeBetweenAndIsActiveTrue(
                        interviewer.getId(), start, end)
                .stream()
                .map(AvailabilitySlotDto::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public AvailabilitySlotDto createAvailabilitySlot(User interviewer, CreateAvailabilitySlotDto dto) {
        if (dto.endDateTime().isBefore(dto.startDateTime())) {
            throw new RuntimeException("End time must be after start time");
        }

        List<AvailabilitySlot> conflicts = availabilitySlotRepository.findConflictingSlots(
                interviewer.getId(),
                dto.startDateTime(),
                dto.endDateTime()
        );

        if (!conflicts.isEmpty()) {
            throw new RuntimeException("This time slot conflicts with existing availability");
        }

        AvailabilitySlot slot = AvailabilitySlot.builder()
                .interviewer(interviewer)
                .startDateTime(dto.startDateTime())
                .endDateTime(dto.endDateTime())
                .description(dto.description())
                .status(SlotStatus.AVAILABLE)
                .isActive(true)
                .build();

        slot = availabilitySlotRepository.save(slot);
        return AvailabilitySlotDto.from(slot);
    }

    /**
     * Update an existing AVAILABLE slot's time range and/or description.
     * Only AVAILABLE (not BOOKED) slots can be edited by the interviewer.
     */
    @Transactional
    public AvailabilitySlotDto updateAvailabilitySlot(User interviewer, Long slotId, UpdateAvailabilitySlotDto dto) {
        AvailabilitySlot slot = availabilitySlotRepository.findById(slotId)
                .orElseThrow(() -> new RuntimeException("Slot not found: " + slotId));

        if (!slot.getInterviewer().getId().equals(interviewer.getId())) {
            throw new RuntimeException("Unauthorized: this slot does not belong to you");
        }

        if (slot.getStatus() == SlotStatus.BOOKED) {
            throw new RuntimeException("Cannot edit a booked slot — it has an interview scheduled");
        }

        if (!slot.isActive()) {
            throw new RuntimeException("Cannot edit an inactive slot");
        }

        if (dto.endDateTime().isBefore(dto.startDateTime()) || dto.endDateTime().isEqual(dto.startDateTime())) {
            throw new RuntimeException("End time must be after start time");
        }

        // Check for conflicts — exclude the current slot from conflict check
        List<AvailabilitySlot> conflicts = availabilitySlotRepository.findConflictingSlots(
                        interviewer.getId(),
                        dto.startDateTime(),
                        dto.endDateTime()
                ).stream()
                .filter(s -> !s.getId().equals(slotId))
                .collect(Collectors.toList());

        if (!conflicts.isEmpty()) {
            throw new RuntimeException("The updated time conflicts with another existing availability slot");
        }

        slot.setStartDateTime(dto.startDateTime());
        slot.setEndDateTime(dto.endDateTime());
        if (dto.description() != null) {
            slot.setDescription(dto.description());
        }

        slot = availabilitySlotRepository.save(slot);
        return AvailabilitySlotDto.from(slot);
    }

    @Transactional
    public List<AvailabilitySlotDto> createBulkAvailabilitySlots(
            User interviewer,
            BulkAvailabilitySlotDto bulkDto
    ) {
        return bulkDto.slots().stream()
                .map(dto -> createAvailabilitySlot(interviewer, dto))
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteAvailabilitySlot(User interviewer, Long slotId) {
        AvailabilitySlot slot = availabilitySlotRepository.findById(slotId)
                .orElseThrow(() -> new RuntimeException("Slot not found"));

        if (!slot.getInterviewer().getId().equals(interviewer.getId())) {
            throw new RuntimeException("Unauthorized");
        }

        if (slot.getStatus() == SlotStatus.BOOKED) {
            throw new RuntimeException("Cannot delete booked slots");
        }

        slot.setActive(false);
        availabilitySlotRepository.save(slot);
    }

    public long getAvailableSlotCount(Long interviewerId) {
        return availabilitySlotRepository.countUpcomingAvailableSlots(interviewerId, LocalDateTime.now());
    }

    public long getBookedSlotCount(Long interviewerId) {
        return availabilitySlotRepository.countUpcomingBookedSlots(interviewerId, LocalDateTime.now());
    }
}