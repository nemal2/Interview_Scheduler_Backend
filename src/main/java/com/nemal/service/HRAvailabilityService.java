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

    /**
     * How far back (in days) the HR calendar shows past slots.
     *
     * Without a lookback the calendar goes empty the moment all current-week
     * slots pass their start time. 30 days keeps recent booked/completed
     * interviews visible so HR can audit what happened.
     */
    private static final int HR_LOOKBACK_DAYS = 30;

    private final AvailabilitySlotRepository availabilitySlotRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public HRAvailabilityService(AvailabilitySlotRepository availabilitySlotRepository) {
        this.availabilitySlotRepository = availabilitySlotRepository;
    }

    /**
     * Returns ALL active slots (AVAILABLE + BOOKED) so the HR calendar shows
     * the full picture — available slots for scheduling and booked slots
     * (greyed out / coloured differently) for awareness.
     *
     * Uses a 30-day lookback so data is never silently lost after slots expire.
     */
    @Transactional
    public List<InterviewerAvailabilityDto> getAllAvailableSlots(AvailabilityFilterDto filter) {
        try {
            List<AvailabilitySlot> slots;

            // "from" is the lookback anchor — 30 days ago by default.
            // When a date filter is applied, the filter's startDateTime acts as the
            // lower bound so we don't need to double-apply the lookback there.
            LocalDateTime from = LocalDateTime.now().minusDays(HR_LOOKBACK_DAYS);

            logger.info("=== HR AVAILABILITY FILTER REQUEST ===");
            logger.info("Filter: {}, lookback from: {}", filter, from);

            if (filter == null) {
                slots = availabilitySlotRepository.findAllActiveSlotsForHR(from);
            } else {
                slots = filterSlots(filter, from);
            }

            logger.info("Total slots (available + booked): {}", slots.size());

            List<InterviewerAvailabilityDto> result = new ArrayList<>();
            for (AvailabilitySlot slot : slots) {
                try {
                    result.add(InterviewerAvailabilityDto.from(slot));
                } catch (Exception e) {
                    logger.error("Error converting slot {} to DTO: {}",
                            slot.getId(), e.getMessage(), e);
                }
            }

            return result;

        } catch (Exception e) {
            logger.error("Error in getAllAvailableSlots: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch available slots: " + e.getMessage(), e);
        }
    }

    private List<AvailabilitySlot> filterSlots(AvailabilityFilterDto filter, LocalDateTime from) {
        List<AvailabilitySlot> slots;

        try {
            // ── Step 1: date range ─────────────────────────────────────────────
            // If the HR user has set an explicit date range, honour it exactly.
            // If no date range is set, fall back to the 30-day lookback window
            // so the calendar always has data.
            if (filter.startDateTime() != null && filter.endDateTime() != null) {
                logger.info("Filtering by explicit date range: {} to {}",
                        filter.startDateTime(), filter.endDateTime());
                slots = availabilitySlotRepository.findAllActiveSlotsForHRByDateRange(
                        filter.startDateTime(), filter.endDateTime());
            } else {
                // No date filter — use lookback so data never vanishes
                slots = availabilitySlotRepository.findAllActiveSlotsForHR(from);
            }
            logger.info("Step 1 – initial slots: {}", slots.size());

            // ── Step 2: department ─────────────────────────────────────────────
            if (filter.departmentIds() != null && !filter.departmentIds().isEmpty()) {
                slots = slots.stream()
                        .filter(slot -> {
                            if (slot.getInterviewer() == null
                                    || slot.getInterviewer().getDepartment() == null)
                                return false;
                            return filter.departmentIds()
                                    .contains(slot.getInterviewer().getDepartment().getId());
                        })
                        .collect(Collectors.toList());
                logger.info("Step 2 – after department filter: {}", slots.size());
            }

            // ── Step 3: technology ─────────────────────────────────────────────
            // Only filters AVAILABLE slots — BOOKED slots are kept as-is so HR
            // can see who is busy even if the tech doesn't match the filter.
            if (filter.technologyIds() != null && !filter.technologyIds().isEmpty()) {
                slots = slots.stream()
                        .filter(slot -> {
                            // Always keep BOOKED slots so the "busy" picture is accurate
                            if ("BOOKED".equals(slot.getStatus().name())) return true;

                            if (slot.getInterviewer() == null) return false;

                            var interviewerTechs = slot.getInterviewer().getInterviewerTechnologies();
                            if (!Hibernate.isInitialized(interviewerTechs)) {
                                Hibernate.initialize(interviewerTechs);
                            }
                            if (interviewerTechs == null || interviewerTechs.isEmpty()) return false;

                            return interviewerTechs.stream()
                                    .filter(it -> it != null
                                            && it.isActive()
                                            && it.getTechnology() != null)
                                    .anyMatch(it -> filter.technologyIds()
                                            .contains(it.getTechnology().getId()));
                        })
                        .collect(Collectors.toList());
                logger.info("Step 3 – after technology filter: {}", slots.size());
            }

            // ── Step 4: years of experience ────────────────────────────────────
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

            // ── Step 5: tier / level hierarchy ─────────────────────────────────
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
                                || slot.getInterviewer().getCurrentDesignation() == null)
                            continue;

                        if (slot.getInterviewer().getDepartment() == null
                                || !slot.getInterviewer().getDepartment().getId()
                                .equals(filter.departmentIdForDesignationFilter()))
                            continue;

                        Designation designation =
                                slot.getInterviewer().getCurrentDesignation();
                        Tier tier = designation.getTier();
                        if (tier == null
                                || tier.getTierOrder() == null
                                || designation.getLevelOrder() == null)
                            continue;

                        int intTier = tier.getTierOrder();
                        int intLevel = designation.getLevelOrder();

                        boolean passes = intTier < minTierOrder
                                || (intTier == minTierOrder && intLevel <= minLevelOrder);

                        if (passes) passedSlots.add(slot);
                    } catch (Exception e) {
                        logger.error("Error checking tier for slot {}: {}",
                                slot.getId(), e.getMessage());
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
                                    || slot.getInterviewer().getCurrentDesignation() == null)
                                return false;
                            if (slot.getInterviewer().getDepartment() == null
                                    || !slot.getInterviewer().getDepartment().getId()
                                    .equals(filter.departmentIdForDesignationFilter()))
                                return false;
                            Integer level =
                                    slot.getInterviewer().getCurrentDesignation().getLevelOrder();
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