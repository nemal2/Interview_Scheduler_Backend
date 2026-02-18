package com.nemal.service;

import com.nemal.dto.CreatePanelInterviewDto;
import com.nemal.dto.InterviewPanelDto;
import com.nemal.entity.*;
import com.nemal.enums.CandidateStatus;
import com.nemal.enums.RequestStatus;
import com.nemal.enums.SlotStatus;
import com.nemal.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PanelInterviewService {

    private static final Logger logger = LoggerFactory.getLogger(PanelInterviewService.class);

    private final InterviewPanelRepository panelRepository;
    private final AvailabilitySlotRepository slotRepository;
    private final InterviewRequestRepository requestRepository;
    private final CandidateRepository candidateRepository;
    private final DesignationRepository designationRepository;
    private final TechnologyRepository technologyRepository;
    private final NotificationService notificationService;

    public PanelInterviewService(
            InterviewPanelRepository panelRepository,
            AvailabilitySlotRepository slotRepository,
            InterviewRequestRepository requestRepository,
            CandidateRepository candidateRepository,
            DesignationRepository designationRepository,
            TechnologyRepository technologyRepository,
            NotificationService notificationService) {
        this.panelRepository = panelRepository;
        this.slotRepository = slotRepository;
        this.requestRepository = requestRepository;
        this.candidateRepository = candidateRepository;
        this.designationRepository = designationRepository;
        this.technologyRepository = technologyRepository;
        this.notificationService = notificationService;
    }

    /**
     * Creates a panel interview: one candidate, multiple interviewers, same time window.
     *
     * For each selected interviewer slot:
     *   1. Validates the slot is AVAILABLE
     *   2. Applies slot splitting (same as single booking)
     *   3. Creates an InterviewRequest linked to the panel
     *   4. Notifies each interviewer
     *
     * All interviewers must have overlapping availability that covers startDateTime–endDateTime.
     */
    @Transactional
    public InterviewPanelDto createPanelInterview(User requestedBy, CreatePanelInterviewDto dto) {
        logger.info("Creating panel interview: candidate='{}', {} interviewers, {} to {}",
                dto.candidateName(), dto.availabilitySlotIds().size(),
                dto.startDateTime(), dto.endDateTime());

        if (dto.availabilitySlotIds() == null || dto.availabilitySlotIds().isEmpty()) {
            throw new RuntimeException("At least one interviewer slot must be selected for a panel");
        }

        // --- 1. Resolve candidate ---
        Candidate candidate = null;
        if (dto.candidateId() != null) {
            candidate = candidateRepository.findById(dto.candidateId())
                    .orElseThrow(() -> new RuntimeException("Candidate not found"));
        }

        String candidateName = candidate != null ? candidate.getName() : dto.candidateName();
        if (candidateName == null || candidateName.trim().isEmpty()) {
            throw new RuntimeException("Candidate name is required");
        }

        // --- 2. Resolve designation ---
        Designation designation = null;
        if (dto.candidateDesignationId() != null) {
            designation = designationRepository.findById(dto.candidateDesignationId())
                    .orElseThrow(() -> new RuntimeException("Designation not found"));
        }

        // --- 3. Resolve technologies ---
        Set<Technology> technologies = new HashSet<>();
        if (dto.requiredTechnologyIds() != null && !dto.requiredTechnologyIds().isEmpty()) {
            technologies = new HashSet<>(technologyRepository.findAllById(dto.requiredTechnologyIds()));
        }

        // --- 4. Validate ALL slots before creating anything (all-or-nothing) ---
        List<AvailabilitySlot> slots = dto.availabilitySlotIds().stream()
                .map(slotId -> {
                    AvailabilitySlot slot = slotRepository.findById(slotId)
                            .orElseThrow(() -> new RuntimeException("Slot not found: " + slotId));
                    if (slot.getStatus() != SlotStatus.AVAILABLE) {
                        throw new RuntimeException(
                                "Slot for " + slot.getInterviewer().getFullName() + " is no longer available");
                    }
                    // Validate booking window fits within this slot
                    if (dto.startDateTime().isBefore(slot.getStartDateTime())) {
                        throw new RuntimeException(
                                "Panel start time is before " + slot.getInterviewer().getFullName() + "'s slot start");
                    }
                    if (dto.endDateTime().isAfter(slot.getEndDateTime())) {
                        throw new RuntimeException(
                                "Panel end time is after " + slot.getInterviewer().getFullName() + "'s slot end");
                    }
                    return slot;
                })
                .collect(Collectors.toList());

        // --- 5. Create the InterviewPanel record ---
        InterviewPanel panel = InterviewPanel.builder()
                .candidate(candidate)
                .candidateName(candidateName)
                .startDateTime(dto.startDateTime())
                .endDateTime(dto.endDateTime())
                .requestedBy(requestedBy)
                .isUrgent(dto.isUrgent())
                .notes(dto.notes())
                .build();
        panel = panelRepository.save(panel);
        logger.info("Created InterviewPanel ID={}", panel.getId());

        // --- 6. For each slot: split + create request ---
        for (AvailabilitySlot slot : slots) {
            // Apply slot splitting
            splitSlot(slot, dto.startDateTime(), dto.endDateTime());

            // Create interview request linked to this panel
            Designation finalDesignation = designation;
            InterviewRequest request = InterviewRequest.builder()
                    .candidateName(candidateName)
                    .candidate(candidate)
                    .candidateDesignation(finalDesignation)
                    .requiredTechnologies(new HashSet<>(technologies))
                    .preferredStartDateTime(dto.startDateTime())
                    .preferredEndDateTime(dto.endDateTime())
                    .requestedBy(requestedBy)
                    .assignedInterviewer(slot.getInterviewer())
                    .availabilitySlot(slot)
                    .panel(panel)
                    .status(RequestStatus.ACCEPTED)
                    .respondedAt(LocalDateTime.now())
                    .isUrgent(dto.isUrgent())
                    .notes(dto.notes())
                    .responseNotes("Auto-accepted as part of panel interview")
                    .build();

            request = requestRepository.save(request);
            logger.info("Created panel request ID={} for interviewer {}", request.getId(),
                    slot.getInterviewer().getFullName());

            // Notify each interviewer
            try {
                notificationService.sendInterviewScheduledNotification(request);
            } catch (Exception e) {
                logger.error("Failed to send notification to {}: {}", slot.getInterviewer().getFullName(), e.getMessage());
            }
        }

        // --- 7. Update candidate status ---
        if (candidate != null) {
            candidate.setStatus(CandidateStatus.SCHEDULED);
            candidateRepository.save(candidate);
        }

        // Reload panel with all requests populated
        InterviewPanel savedPanel = panelRepository.findByIdWithDetails(panel.getId())
                .orElseThrow(() -> new RuntimeException("Panel not found after save"));
        return InterviewPanelDto.from(savedPanel);
    }

    /**
     * Cancels all requests in a panel and restores all slots to AVAILABLE.
     */
    @Transactional
    public void cancelPanelInterview(User hrUser, Long panelId) {
        InterviewPanel panel = panelRepository.findByIdWithDetails(panelId)
                .orElseThrow(() -> new RuntimeException("Panel not found"));

        if (!panel.getRequestedBy().getId().equals(hrUser.getId())) {
            throw new RuntimeException("Unauthorized — you did not create this panel");
        }

        for (InterviewRequest request : panel.getPanelRequests()) {
            if (request.getStatus() == RequestStatus.ACCEPTED) {
                AvailabilitySlot slot = request.getAvailabilitySlot();
                if (slot != null) {
                    slot.setStatus(SlotStatus.AVAILABLE);
                    slotRepository.save(slot);
                }
                request.setStatus(RequestStatus.CANCELLED);
                requestRepository.save(request);

                try {
                    notificationService.sendInterviewCancelledNotification(request);
                } catch (Exception e) {
                    logger.error("Failed to send cancellation notification: {}", e.getMessage());
                }
            }
        }

        // Revert candidate status if all interviews are cancelled
        if (panel.getCandidate() != null) {
            panel.getCandidate().setStatus(CandidateStatus.SCREENING);
            candidateRepository.save(panel.getCandidate());
        }

        logger.info("Cancelled panel {} with {} requests", panelId, panel.getPanelRequests().size());
    }

    public List<InterviewPanelDto> getPanelsByCandidateId(Long candidateId) {
        return panelRepository.findByCandidateId(candidateId)
                .stream()
                .map(InterviewPanelDto::from)
                .collect(Collectors.toList());
    }

    public List<InterviewPanelDto> getPanelsByRequestedBy(Long userId) {
        return panelRepository.findByRequestedById(userId)
                .stream()
                .map(InterviewPanelDto::from)
                .collect(Collectors.toList());
    }

    /**
     * Splits an availability slot around a booking window.
     *
     * Before: [slotStart ————————————————— slotEnd]
     * Book:             [bookStart — bookEnd]
     * After:  [slotStart — bookStart] [bookStart — bookEnd] [bookEnd — slotEnd]
     *              (AVAILABLE)              (BOOKED)            (AVAILABLE)
     */
    private void splitSlot(AvailabilitySlot slot, LocalDateTime bookStart, LocalDateTime bookEnd) {
        LocalDateTime slotStart = slot.getStartDateTime();
        LocalDateTime slotEnd = slot.getEndDateTime();

        // Prefix: time before booking window
        if (bookStart.isAfter(slotStart)) {
            AvailabilitySlot prefix = AvailabilitySlot.builder()
                    .interviewer(slot.getInterviewer())
                    .startDateTime(slotStart)
                    .endDateTime(bookStart)
                    .description(slot.getDescription())
                    .status(SlotStatus.AVAILABLE)
                    .isActive(true)
                    .build();
            slotRepository.save(prefix);
            logger.info("Split prefix: {} to {} AVAILABLE for {}",
                    slotStart, bookStart, slot.getInterviewer().getFullName());
        }

        // Suffix: time after booking window
        if (bookEnd.isBefore(slotEnd)) {
            AvailabilitySlot suffix = AvailabilitySlot.builder()
                    .interviewer(slot.getInterviewer())
                    .startDateTime(bookEnd)
                    .endDateTime(slotEnd)
                    .description(slot.getDescription())
                    .status(SlotStatus.AVAILABLE)
                    .isActive(true)
                    .build();
            slotRepository.save(suffix);
            logger.info("Split suffix: {} to {} AVAILABLE for {}",
                    bookEnd, slotEnd, slot.getInterviewer().getFullName());
        }

        // Original slot → trimmed to booked window
        slot.setStartDateTime(bookStart);
        slot.setEndDateTime(bookEnd);
        slot.setStatus(SlotStatus.BOOKED);
        slotRepository.save(slot);
        logger.info("Slot {} trimmed to {} – {} (BOOKED)", slot.getId(), bookStart, bookEnd);
    }
}