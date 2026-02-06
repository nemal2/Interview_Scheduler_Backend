package com.nemal.service;

import com.nemal.dto.AvailabilityFilterDto;
import com.nemal.dto.InterviewerAvailabilityDto;
import com.nemal.entity.AvailabilitySlot;
import com.nemal.entity.InterviewerTechnology;
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
                                if (slot.getInterviewer() == null) {
                                    logger.warn("Slot {} has null interviewer", slot.getId());
                                    return false;
                                }
                                if (slot.getInterviewer().getDepartment() == null) {
                                    logger.warn("Interviewer {} has null department", slot.getInterviewer().getId());
                                    return false;
                                }
                                boolean matches = filter.departmentIds().contains(
                                        slot.getInterviewer().getDepartment().getId());
                                if (!matches) {
                                    logger.debug("Slot {} filtered out - department {} not in filter {}",
                                            slot.getId(),
                                            slot.getInterviewer().getDepartment().getId(),
                                            filter.departmentIds());
                                }
                                return matches;
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
                                if (slot.getInterviewer() == null) {
                                    return false;
                                }

                                var interviewerTechs = slot.getInterviewer().getInterviewerTechnologies();
                                if (interviewerTechs == null || interviewerTechs.isEmpty()) {
                                    logger.debug("Interviewer {} has no technologies", slot.getInterviewer().getId());
                                    return false;
                                }

                                boolean hasMatchingTech = interviewerTechs.stream()
                                        .filter(it -> it != null && it.isActive() && it.getTechnology() != null)
                                        .anyMatch(it -> filter.technologyIds().contains(it.getTechnology().getId()));

                                if (!hasMatchingTech) {
                                    logger.debug("Slot {} filtered out - no matching technology", slot.getId());
                                }
                                return hasMatchingTech;
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
                                if (slot.getInterviewer() == null) {
                                    return false;
                                }
                                Integer years = slot.getInterviewer().getYearsOfExperience();
                                if (years == null) {
                                    logger.debug("Interviewer {} has null years of experience", slot.getInterviewer().getId());
                                    return false;
                                }
                                boolean matches = years >= filter.minYearsOfExperience();
                                if (!matches) {
                                    logger.debug("Slot {} filtered out - experience {} < required {}",
                                            slot.getId(), years, filter.minYearsOfExperience());
                                }
                                return matches;
                            } catch (Exception e) {
                                logger.warn("Error checking experience for slot {}: {}", slot.getId(), e.getMessage());
                                return false;
                            }
                        })
                        .collect(Collectors.toList());
                logger.info("After experience filter: {} slots", slots.size());
            }

            // Apply designation level filter
            if (filter.minDesignationLevelInDepartment() != null &&
                    filter.departmentIdForDesignationFilter() != null) {
                logger.info("Filtering by min designation level: {} in department: {}",
                        filter.minDesignationLevelInDepartment(), filter.departmentIdForDesignationFilter());
                slots = slots.stream()
                        .filter(slot -> {
                            try {
                                if (slot.getInterviewer() == null ||
                                        slot.getInterviewer().getCurrentDesignation() == null) {
                                    logger.debug("Slot {} has no designation", slot.getId());
                                    return false;
                                }

                                var designation = slot.getInterviewer().getCurrentDesignation();

                                // Check if interviewer is in the specified department
                                if (slot.getInterviewer().getDepartment() == null ||
                                        !slot.getInterviewer().getDepartment().getId()
                                                .equals(filter.departmentIdForDesignationFilter())) {
                                    logger.debug("Slot {} filtered out - wrong department", slot.getId());
                                    return false;
                                }

                                // Check if designation level is >= minimum
                                if (designation.getLevelOrder() == null) {
                                    logger.debug("Designation {} has null level order", designation.getId());
                                    return false;
                                }

                                boolean matches = designation.getLevelOrder() >= filter.minDesignationLevelInDepartment();
                                if (!matches) {
                                    logger.debug("Slot {} filtered out - level {} < required {}",
                                            slot.getId(), designation.getLevelOrder(), filter.minDesignationLevelInDepartment());
                                }
                                return matches;
                            } catch (Exception e) {
                                logger.warn("Error checking designation level for slot {}: {}",
                                        slot.getId(), e.getMessage());
                                return false;
                            }
                        })
                        .collect(Collectors.toList());
                logger.info("After designation level filter: {} slots", slots.size());
            }

            // Apply tier filter
            if (filter.minTierId() != null) {
                logger.info("Filtering by min tier order: {}", filter.minTierId());
                slots = slots.stream()
                        .filter(slot -> {
                            try {
                                if (slot.getInterviewer() == null ||
                                        slot.getInterviewer().getCurrentDesignation() == null ||
                                        slot.getInterviewer().getCurrentDesignation().getTier() == null) {
                                    logger.debug("Slot {} has no tier", slot.getId());
                                    return false;
                                }

                                var tier = slot.getInterviewer().getCurrentDesignation().getTier();

                                // Check if tier order is >= minimum tier order
                                if (tier.getTierOrder() == null) {
                                    logger.debug("Tier {} has null tier order", tier.getId());
                                    return false;
                                }

                                boolean matches = tier.getTierOrder() >= filter.minTierId();
                                if (!matches) {
                                    logger.debug("Slot {} filtered out - tier {} < required {}",
                                            slot.getId(), tier.getTierOrder(), filter.minTierId());
                                }
                                return matches;
                            } catch (Exception e) {
                                logger.warn("Error checking tier for slot {}: {}", slot.getId(), e.getMessage());
                                return false;
                            }
                        })
                        .collect(Collectors.toList());
                logger.info("After tier filter: {} slots", slots.size());
            }

            return slots;
        } catch (Exception e) {
            logger.error("Error filtering slots: {}", e.getMessage(), e);
            throw new RuntimeException("Error filtering availability slots", e);
        }
    }
}