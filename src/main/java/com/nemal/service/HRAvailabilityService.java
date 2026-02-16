package com.nemal.service;

import com.nemal.dto.AvailabilityFilterDto;
import com.nemal.dto.InterviewerAvailabilityDto;
import com.nemal.entity.AvailabilitySlot;
import com.nemal.entity.Designation;
import com.nemal.entity.InterviewerTechnology;
import com.nemal.entity.Tier;
import com.nemal.repository.AvailabilitySlotRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class HRAvailabilityService {

    private static final Logger logger = LoggerFactory.getLogger(HRAvailabilityService.class);
    private final AvailabilitySlotRepository availabilitySlotRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public HRAvailabilityService(AvailabilitySlotRepository availabilitySlotRepository) {
        this.availabilitySlotRepository = availabilitySlotRepository;
    }

    @Transactional
    public List<InterviewerAvailabilityDto> getAllAvailableSlots(AvailabilityFilterDto filter) {
        try {
            List<AvailabilitySlot> slots;
            LocalDateTime now = LocalDateTime.now();

            logger.info("=== FILTER REQUEST START ===");
            logger.info("Filter: {}", filter);

            if (filter == null) {
                slots = availabilitySlotRepository.findAllAvailableSlots(now);
            } else {
                slots = filterSlots(filter, now);
            }

            logger.info("Final result: {} slots", slots.size());
            logger.info("=== FILTER REQUEST END ===");

            // Convert to DTOs with error handling for each slot
            List<InterviewerAvailabilityDto> result = new ArrayList<>();
            for (AvailabilitySlot slot : slots) {
                try {
                    InterviewerAvailabilityDto dto = InterviewerAvailabilityDto.from(slot);
                    result.add(dto);
                } catch (Exception e) {
                    logger.error("Error converting slot {} to DTO: {}", slot.getId(), e.getMessage(), e);
                }
            }

            return result;

        } catch (Exception e) {
            logger.error("Error in getAllAvailableSlots: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch available slots: " + e.getMessage(), e);
        }
    }

    private List<AvailabilitySlot> filterSlots(AvailabilityFilterDto filter, LocalDateTime now) {
        List<AvailabilitySlot> slots;

        try {
            // CRITICAL: Clear the persistence context to avoid stale data
            entityManager.clear();

            // First apply date range if provided
            if (filter.startDateTime() != null && filter.endDateTime() != null) {
                logger.info("Filtering by date range: {} to {}", filter.startDateTime(), filter.endDateTime());
                slots = availabilitySlotRepository.findAllAvailableSlotsByDateRange(
                        filter.startDateTime(), filter.endDateTime());
            } else {
                logger.info("Fetching all available slots from now: {}", now);
                slots = availabilitySlotRepository.findAllAvailableSlots(now);
            }

            logger.info("Step 1 - Initial slots: {}", slots.size());

            // Apply department filter
            if (filter.departmentIds() != null && !filter.departmentIds().isEmpty()) {
                logger.info("Step 2 - Filtering by departments: {}", filter.departmentIds());
                slots = slots.stream()
                        .filter(slot -> {
                            if (slot.getInterviewer() == null || slot.getInterviewer().getDepartment() == null) {
                                return false;
                            }
                            return filter.departmentIds().contains(slot.getInterviewer().getDepartment().getId());
                        })
                        .collect(Collectors.toList());
                logger.info("Step 2 - After department filter: {} slots", slots.size());
            }

            // Apply technology filter with DETAILED DEBUGGING
            if (filter.technologyIds() != null && !filter.technologyIds().isEmpty()) {
                logger.info("Step 3 - Filtering by technologies: {}", filter.technologyIds());

                List<AvailabilitySlot> passedSlots = new ArrayList<>();

                for (AvailabilitySlot slot : slots) {
                    try {
                        if (slot.getInterviewer() == null) {
                            logger.info("  Slot {} - SKIP: null interviewer", slot.getId());
                            continue;
                        }

                        Long interviewerId = slot.getInterviewer().getId();

                        logger.info("  Slot {} - Interviewer {}", slot.getId(), interviewerId);

                        // CRITICAL: Force Hibernate to initialize the lazy collection
                        Set<InterviewerTechnology> interviewerTechs = slot.getInterviewer().getInterviewerTechnologies();

                        logger.info("    → Collection class: {}", interviewerTechs.getClass().getName());
                        logger.info("    → Collection size BEFORE initialize: {}", interviewerTechs.size());

                        Hibernate.initialize(interviewerTechs);

                        logger.info("    → Collection size AFTER initialize: {}", interviewerTechs.size());
                        logger.info("    → Is empty: {}", interviewerTechs.isEmpty());

                        if (interviewerTechs == null || interviewerTechs.isEmpty()) {
                            logger.info("    → Has NO technologies");
                            continue;
                        }

                        // Extract technology IDs
                        List<Long> interviewerTechIds = new ArrayList<>();
                        for (InterviewerTechnology it : interviewerTechs) {
                            if (it != null && it.isActive() && it.getTechnology() != null) {
                                interviewerTechIds.add(it.getTechnology().getId());
                            }
                        }

                        logger.info("    → Has technologies: {}", interviewerTechIds);
                        logger.info("    → Filter requires: {}", filter.technologyIds());

                        // Check if ANY match
                        boolean hasMatch = false;
                        for (Long requiredTechId : filter.technologyIds()) {
                            if (interviewerTechIds.contains(requiredTechId)) {
                                hasMatch = true;
                                logger.info("    → ✓ MATCH FOUND: Technology ID {}", requiredTechId);
                                break;
                            }
                        }

                        if (hasMatch) {
                            passedSlots.add(slot);
                            logger.info("    → ✓✓ SLOT PASSED");
                        } else {
                            logger.info("    → ✗✗ SLOT FILTERED OUT");
                        }

                    } catch (Exception e) {
                        logger.error("Error checking slot {}: {}", slot.getId(), e.getMessage(), e);
                    }
                }

                slots = passedSlots;
                logger.info("Step 3 - After technology filter: {} slots", slots.size());
            }

            // Apply years of experience filter
            if (filter.minYearsOfExperience() != null) {
                logger.info("Step 4 - Filtering by min experience: {}", filter.minYearsOfExperience());
                slots = slots.stream()
                        .filter(slot -> {
                            if (slot.getInterviewer() == null) return false;
                            Integer years = slot.getInterviewer().getYearsOfExperience();
                            if (years == null) return false;
                            return years >= filter.minYearsOfExperience();
                        })
                        .collect(Collectors.toList());
                logger.info("Step 4 - After experience filter: {} slots", slots.size());
            }

            // Apply tier + level hierarchy filter (ONLY if departmentIdForDesignationFilter is set)
            if (filter.minTierId() != null && filter.departmentIdForDesignationFilter() != null) {
                logger.info("Step 5 - Filtering by tier/level hierarchy");
                logger.info("  Department: {}", filter.departmentIdForDesignationFilter());
                logger.info("  Min Tier Order: {}", filter.minTierId());
                logger.info("  Min Level Order: {}", filter.minDesignationLevelInDepartment());

                // Convert Long to int if needed
                final int minTierOrder = filter.minTierId().intValue();
                final int minLevelOrder = filter.minDesignationLevelInDepartment() != null
                        ? filter.minDesignationLevelInDepartment().intValue()
                        : Integer.MAX_VALUE;

                List<AvailabilitySlot> passedSlots = new ArrayList<>();

                for (AvailabilitySlot slot : slots) {
                    try {
                        if (slot.getInterviewer() == null || slot.getInterviewer().getCurrentDesignation() == null) {
                            logger.debug("  Slot {} - SKIP: No designation", slot.getId());
                            continue;
                        }

                        Designation designation = slot.getInterviewer().getCurrentDesignation();

                        // Check department match
                        if (slot.getInterviewer().getDepartment() == null ||
                                !slot.getInterviewer().getDepartment().getId()
                                        .equals(filter.departmentIdForDesignationFilter())) {
                            logger.debug("  Slot {} - SKIP: Wrong department", slot.getId());
                            continue;
                        }

                        Tier tier = designation.getTier();
                        if (tier == null || tier.getTierOrder() == null || designation.getLevelOrder() == null) {
                            logger.debug("  Slot {} - SKIP: Invalid tier/level", slot.getId());
                            continue;
                        }

                        int interviewerTierOrder = tier.getTierOrder();
                        int interviewerLevelOrder = designation.getLevelOrder();

                        logger.debug("  Slot {} - Interviewer: Tier {} Level {}",
                                slot.getId(), interviewerTierOrder, interviewerLevelOrder);
                        logger.debug("    Required: Tier {} Level {}", minTierOrder, minLevelOrder);

                        boolean passes = false;

                        if (interviewerTierOrder < minTierOrder) {
                            // Higher tier (lower number)
                            passes = true;
                            logger.debug("    → ✓ PASS: Higher tier");
                        } else if (interviewerTierOrder == minTierOrder) {
                            // Same tier - check level
                            if (interviewerLevelOrder <= minLevelOrder) {
                                passes = true;
                                logger.debug("    → ✓ PASS: Same tier, sufficient level");
                            } else {
                                logger.debug("    → ✗ FAIL: Same tier, insufficient level");
                            }
                        } else {
                            // Lower tier (higher number)
                            logger.debug("    → ✗ FAIL: Lower tier");
                        }

                        if (passes) {
                            passedSlots.add(slot);
                        }

                    } catch (Exception e) {
                        logger.error("Error checking tier for slot {}: {}", slot.getId(), e.getMessage());
                    }
                }

                slots = passedSlots;
                logger.info("Step 5 - After tier/level filter: {} slots", slots.size());
            }
            // If only level specified (no tier)
            else if (filter.minDesignationLevelInDepartment() != null &&
                    filter.departmentIdForDesignationFilter() != null) {
                logger.info("Step 5 - Filtering by level only: {}", filter.minDesignationLevelInDepartment());

                final int minLevel = filter.minDesignationLevelInDepartment().intValue();

                slots = slots.stream()
                        .filter(slot -> {
                            if (slot.getInterviewer() == null ||
                                    slot.getInterviewer().getCurrentDesignation() == null) {
                                return false;
                            }

                            if (slot.getInterviewer().getDepartment() == null ||
                                    !slot.getInterviewer().getDepartment().getId()
                                            .equals(filter.departmentIdForDesignationFilter())) {
                                return false;
                            }

                            Integer level = slot.getInterviewer().getCurrentDesignation().getLevelOrder();
                            return level != null && level <= minLevel;
                        })
                        .collect(Collectors.toList());
                logger.info("Step 5 - After level filter: {} slots", slots.size());
            }

            return slots;

        } catch (Exception e) {
            logger.error("Error filtering slots: {}", e.getMessage(), e);
            throw new RuntimeException("Error filtering availability slots", e);
        }
    }
}