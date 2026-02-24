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

    /**
     * Returns ALL active slots (AVAILABLE + BOOKED) so the HR calendar shows
     * the full picture – available slots for scheduling and booked slots
     * (greyed out / coloured differently) for awareness.
     */
    @Transactional
    public List<InterviewerAvailabilityDto> getAllAvailableSlots(AvailabilityFilterDto filter) {
        try {
            List<AvailabilitySlot> slots;
            LocalDateTime now = LocalDateTime.now();

            logger.info("=== HR AVAILABILITY FILTER REQUEST ===");
            logger.info("Filter: {}", filter);

            if (filter == null) {
                slots = availabilitySlotRepository.findAllActiveSlotsForHR(now);
            } else {
                slots = filterSlots(filter, now);
            }

            logger.info("Total slots (available + booked): {}", slots.size());

            List<InterviewerAvailabilityDto> result = new ArrayList<>();
            for (AvailabilitySlot slot : slots) {
                try {
                    result.add(InterviewerAvailabilityDto.from(slot));
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
            // Step 1 – date range
            if (filter.startDateTime() != null && filter.endDateTime() != null) {
                logger.info("Filtering by date range: {} to {}", filter.startDateTime(), filter.endDateTime());
                slots = availabilitySlotRepository.findAllActiveSlotsForHRByDateRange(
                        filter.startDateTime(), filter.endDateTime());
            } else {
                slots = availabilitySlotRepository.findAllActiveSlotsForHR(now);
            }
            logger.info("Step 1 – initial slots: {}", slots.size());

            // Step 2 – department
            if (filter.departmentIds() != null && !filter.departmentIds().isEmpty()) {
                slots = slots.stream()
                        .filter(slot -> {
                            if (slot.getInterviewer() == null || slot.getInterviewer().getDepartment() == null)
                                return false;
                            return filter.departmentIds().contains(slot.getInterviewer().getDepartment().getId());
                        })
                        .collect(Collectors.toList());
                logger.info("Step 2 – after department filter: {}", slots.size());
            }

            // Step 3 – technology (only applies to AVAILABLE slots for filtering;
            //          BOOKED slots are kept as-is so HR can see who is busy)
            if (filter.technologyIds() != null && !filter.technologyIds().isEmpty()) {
                slots = slots.stream()
                        .filter(slot -> {
                            // Always keep BOOKED slots in the result
                            if ("BOOKED".equals(slot.getStatus().name())) return true;

                            if (slot.getInterviewer() == null) return false;

                            var interviewerTechs = slot.getInterviewer().getInterviewerTechnologies();
                            if (!Hibernate.isInitialized(interviewerTechs)) {
                                Hibernate.initialize(interviewerTechs);
                            }
                            if (interviewerTechs == null || interviewerTechs.isEmpty()) return false;

                            return interviewerTechs.stream()
                                    .filter(it -> it != null && it.isActive() && it.getTechnology() != null)
                                    .anyMatch(it -> filter.technologyIds().contains(it.getTechnology().getId()));
                        })
                        .collect(Collectors.toList());
                logger.info("Step 3 – after technology filter: {}", slots.size());
            }

            // Step 4 – years of experience (skip BOOKED)
            if (filter.minYearsOfExperience() != null) {
                slots = slots.stream()
                        .filter(slot -> {
                            if ("BOOKED".equals(slot.getStatus().name())) return true;
                            if (slot.getInterviewer() == null) return false;
                            Integer years = slot.getInterviewer().getYearsOfExperience();
                            return years != null && years >= filter.minYearsOfExperience();
                        })
                        .collect(Collectors.toList());
                logger.info("Step 4 – after experience filter: {}", slots.size());
            }

            // Step 5 – tier / level hierarchy (skip BOOKED)
            if (filter.minTierId() != null && filter.departmentIdForDesignationFilter() != null) {
                final int minTierOrder = filter.minTierId().intValue();
                final int minLevelOrder = filter.minDesignationLevelInDepartment() != null
                        ? filter.minDesignationLevelInDepartment().intValue()
                        : Integer.MAX_VALUE;

                List<AvailabilitySlot> passedSlots = new ArrayList<>();
                for (AvailabilitySlot slot : slots) {
                    if ("BOOKED".equals(slot.getStatus().name())) {
                        passedSlots.add(slot);
                        continue;
                    }
                    try {
                        if (slot.getInterviewer() == null
                                || slot.getInterviewer().getCurrentDesignation() == null) continue;

                        if (slot.getInterviewer().getDepartment() == null
                                || !slot.getInterviewer().getDepartment().getId()
                                .equals(filter.departmentIdForDesignationFilter())) continue;

                        Designation designation = slot.getInterviewer().getCurrentDesignation();
                        Tier tier = designation.getTier();
                        if (tier == null || tier.getTierOrder() == null
                                || designation.getLevelOrder() == null) continue;

                        int intTier = tier.getTierOrder();
                        int intLevel = designation.getLevelOrder();

                        boolean passes = intTier < minTierOrder
                                || (intTier == minTierOrder && intLevel <= minLevelOrder);

                        if (passes) passedSlots.add(slot);
                    } catch (Exception e) {
                        logger.error("Error checking tier for slot {}: {}", slot.getId(), e.getMessage());
                    }
                }
                slots = passedSlots;
                logger.info("Step 5 – after tier/level filter: {}", slots.size());

            } else if (filter.minDesignationLevelInDepartment() != null
                    && filter.departmentIdForDesignationFilter() != null) {

                final int minLevel = filter.minDesignationLevelInDepartment().intValue();
                slots = slots.stream()
                        .filter(slot -> {
                            if ("BOOKED".equals(slot.getStatus().name())) return true;
                            if (slot.getInterviewer() == null
                                    || slot.getInterviewer().getCurrentDesignation() == null) return false;
                            if (slot.getInterviewer().getDepartment() == null
                                    || !slot.getInterviewer().getDepartment().getId()
                                    .equals(filter.departmentIdForDesignationFilter())) return false;
                            Integer level = slot.getInterviewer().getCurrentDesignation().getLevelOrder();
                            return level != null && level <= minLevel;
                        })
                        .collect(Collectors.toList());
                logger.info("Step 5 – after level filter: {}", slots.size());
            }

            return slots;

        } catch (Exception e) {
            logger.error("Error filtering slots: {}", e.getMessage(), e);
            throw new RuntimeException("Error filtering availability slots", e);
        }
    }
}