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
     * For each slot: splits it around the booking window and links the new BOOKED portion
     * to the created InterviewRequest so both HR and interviewer calendars show it correctly.
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
        Designation finalDesignation = designation;
        String finalCandidateName = candidateName;
        Set<Technology> finalTechnologies = technologies;

        for (AvailabilitySlot slot : slots) {
            // Split and get the active BOOKED slot
            AvailabilitySlot bookedSlot = splitSlot(slot, dto.startDateTime(), dto.endDateTime(), finalCandidateName);

            InterviewRequest request = InterviewRequest.builder()
                    .candidateName(finalCandidateName)
                    .candidate(candidate)
                    .candidateDesignation(finalDesignation)
                    .requiredTechnologies(new HashSet<>(finalTechnologies))
                    .preferredStartDateTime(dto.startDateTime())
                    .preferredEndDateTime(dto.endDateTime())
                    .requestedBy(requestedBy)
                    .assignedInterviewer(slot.getInterviewer())
                    .availabilitySlot(bookedSlot)      // ← link to the active BOOKED slot
                    .panel(panel)
                    .status(RequestStatus.ACCEPTED)
                    .respondedAt(LocalDateTime.now())
                    .isUrgent(dto.isUrgent())
                    .notes(dto.notes())
                    .responseNotes("Auto-accepted as part of panel interview")
                    .build();

            request = requestRepository.save(request);
            logger.info("Created panel request ID={} for interviewer {}",
                    request.getId(), slot.getInterviewer().getFullName());

            try {
                notificationService.sendInterviewScheduledNotification(request);
            } catch (Exception e) {
                logger.error("Failed to send notification to {}: {}",
                        slot.getInterviewer().getFullName(), e.getMessage());
            }
        }

        // --- 7. Update candidate status ---
        if (candidate != null) {
            candidate.setStatus(CandidateStatus.SCHEDULED);
            candidateRepository.save(candidate);
        }

        InterviewPanel savedPanel = panelRepository.findByIdWithDetails(panel.getId())
                .orElseThrow(() -> new RuntimeException("Panel not found after save"));
        return InterviewPanelDto.from(savedPanel);
    }

    /**
     * Cancels all requests in a panel and marks their booked slots back to AVAILABLE.
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
                    slot.setDescription(null);
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

        if (panel.getCandidate() != null) {
            panel.getCandidate().setStatus(CandidateStatus.SCREENING);
            candidateRepository.save(panel.getCandidate());
        }

        logger.info("Cancelled panel {} with {} requests",
                panelId, panel.getPanelRequests().size());
    }

    public List<InterviewPanelDto> getPanelsByCandidateId(Long candidateId) {
        return panelRepository.findByCandidateId(candidateId)
                .stream().map(InterviewPanelDto::from).collect(Collectors.toList());
    }

    public List<InterviewPanelDto> getPanelsByRequestedBy(Long userId) {
        return panelRepository.findByRequestedById(userId)
                .stream().map(InterviewPanelDto::from).collect(Collectors.toList());
    }

    /**
     * Splits an availability slot around a booking window and returns the active BOOKED slot.
     *
     * <pre>
     * Full booking:
     *   Original slot → status BOOKED, description updated, returned as-is
     *
     * Partial booking:
     *   Before:  [slotStart ─────────────────────── slotEnd]
     *   After:   [slotStart─bookStart) AVAILABLE
     *            [bookStart─bookEnd]   BOOKED  ← returned
     *            (bookEnd─slotEnd]     AVAILABLE
     * </pre>
     */
    private AvailabilitySlot splitSlot(AvailabilitySlot slot,
                                       LocalDateTime bookStart,
                                       LocalDateTime bookEnd,
                                       String candidateName) {
        LocalDateTime slotStart = slot.getStartDateTime();
        LocalDateTime slotEnd = slot.getEndDateTime();
        boolean isFullBooking = bookStart.equals(slotStart) && bookEnd.equals(slotEnd);

        if (isFullBooking) {
            slot.setStatus(SlotStatus.BOOKED);
            slot.setDescription("Panel Interview: " + candidateName);
            return slotRepository.save(slot);
        }

        // Partial booking — deactivate original
        slot.setActive(false);
        slotRepository.save(slot);

        // Prefix
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

        // Booked window
        AvailabilitySlot bookedSlot = AvailabilitySlot.builder()
                .interviewer(slot.getInterviewer())
                .startDateTime(bookStart)
                .endDateTime(bookEnd)
                .description("Panel Interview: " + candidateName)
                .status(SlotStatus.BOOKED)
                .isActive(true)
                .build();
        bookedSlot = slotRepository.save(bookedSlot);
        logger.info("Created BOOKED slot {} – {} for {}",
                bookStart, bookEnd, slot.getInterviewer().getFullName());

        // Suffix
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

        return bookedSlot;
    }
}