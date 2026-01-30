package com.nemal.service;

import com.nemal.dto.AvailabilityFilterDto;
import com.nemal.dto.InterviewerAvailabilityDto;
import com.nemal.entity.AvailabilitySlot;
import com.nemal.repository.AvailabilitySlotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class HRAvailabilityService {

    private static final Logger logger = LoggerFactory.getLogger(HRAvailabilityService.class);
    private final AvailabilitySlotRepository availabilitySlotRepository;

    public HRAvailabilityService(AvailabilitySlotRepository availabilitySlotRepository) {
        this.availabilitySlotRepository = availabilitySlotRepository;
    }

    @Transactional(readOnly = true)
    public List<InterviewerAvailabilityDto> getAllAvailableSlots(AvailabilityFilterDto filter) {
        try {
            List<AvailabilitySlot> slots;
            LocalDateTime now = LocalDateTime.now();

            logger.info("Fetching available slots with filter: {}", filter);

            if (filter == null) {
                slots = availabilitySlotRepository.findAllAvailableSlots(now);
            } else {
                slots = filterSlots(filter, now);
            }

            logger.info("Found {} slots before DTO conversion", slots.size());

            // Convert to DTOs with error handling for each slot
            List<InterviewerAvailabilityDto> result = new ArrayList<>();
            for (AvailabilitySlot slot : slots) {
                try {
                    InterviewerAvailabilityDto dto = InterviewerAvailabilityDto.from(slot);
                    result.add(dto);
                } catch (Exception e) {
                    logger.error("Error converting slot {} to DTO: {}", slot.getId(), e.getMessage(), e);
                    // Continue with next slot instead of failing entire request
                }
            }

            logger.info("Successfully converted {} slots to DTOs", result.size());
            return result;

        } catch (Exception e) {
            logger.error("Error in getAllAvailableSlots: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch available slots: " + e.getMessage(), e);
        }
    }

    private List<AvailabilitySlot> filterSlots(AvailabilityFilterDto filter, LocalDateTime now) {
        List<AvailabilitySlot> slots;

        try {
            // First apply date range if provided
            if (filter.startDateTime() != null && filter.endDateTime() != null) {
                logger.info("Filtering by date range: {} to {}", filter.startDateTime(), filter.endDateTime());
                slots = availabilitySlotRepository.findAllAvailableSlotsByDateRange(
                        filter.startDateTime(), filter.endDateTime());
            } else {
                logger.info("Fetching all available slots from now: {}", now);
                slots = availabilitySlotRepository.findAllAvailableSlots(now);
            }

            logger.info("Initial slots count: {}", slots.size());

            // Apply department filter
            if (filter.departmentIds() != null && !filter.departmentIds().isEmpty()) {
                logger.info("Filtering by departments: {}", filter.departmentIds());
                slots = slots.stream()
                        .filter(slot -> {
                            try {
                                return slot.getInterviewer() != null &&
                                        slot.getInterviewer().getDepartment() != null &&
                                        filter.departmentIds().contains(slot.getInterviewer().getDepartment().getId());
                            } catch (Exception e) {
                                logger.warn("Error checking department for slot {}: {}", slot.getId(), e.getMessage());
                                return false;
                            }
                        })
                        .collect(Collectors.toList());
                logger.info("After department filter: {} slots", slots.size());
            }

            // Apply technology filter
            if (filter.technologyIds() != null && !filter.technologyIds().isEmpty()) {
                logger.info("Filtering by technologies: {}", filter.technologyIds());
                slots = slots.stream()
                        .filter(slot -> {
                            try {
                                return slot.getInterviewer() != null &&
                                        slot.getInterviewer().getInterviewerTechnologies() != null &&
                                        slot.getInterviewer().getInterviewerTechnologies().stream()
                                                .anyMatch(it -> it != null &&
                                                        it.getTechnology() != null &&
                                                        filter.technologyIds().contains(it.getTechnology().getId()));
                            } catch (Exception e) {
                                logger.warn("Error checking technologies for slot {}: {}", slot.getId(), e.getMessage());
                                return false;
                            }
                        })
                        .collect(Collectors.toList());
                logger.info("After technology filter: {} slots", slots.size());
            }

            // Apply years of experience filter
            if (filter.minYearsOfExperience() != null) {
                logger.info("Filtering by min experience: {}", filter.minYearsOfExperience());
                slots = slots.stream()
                        .filter(slot -> {
                            try {
                                return slot.getInterviewer() != null &&
                                        slot.getInterviewer().getYearsOfExperience() != null &&
                                        slot.getInterviewer().getYearsOfExperience() >= filter.minYearsOfExperience();
                            } catch (Exception e) {
                                logger.warn("Error checking experience for slot {}: {}", slot.getId(), e.getMessage());
                                return false;
                            }
                        })
                        .collect(Collectors.toList());
                logger.info("After experience filter: {} slots", slots.size());
            }

            return slots;
        } catch (Exception e) {
            logger.error("Error filtering slots: {}", e.getMessage(), e);
            throw new RuntimeException("Error filtering availability slots", e);
        }
    }
}