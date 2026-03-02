package com.nemal.service;

import com.nemal.dto.CreatePanelInterviewDto;
import com.nemal.dto.InterviewPanelDto;
import com.nemal.entity.*;
import com.nemal.enums.CandidateStatus;
import com.nemal.enums.InterviewStatus;
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
    private final InterviewScheduleRepository scheduleRepository;
    private final CandidateRepository candidateRepository;
    private final DesignationRepository designationRepository;
    private final TechnologyRepository technologyRepository;
    private final NotificationService notificationService;

    public PanelInterviewService(
            InterviewPanelRepository panelRepository,
            AvailabilitySlotRepository slotRepository,
            InterviewRequestRepository requestRepository,
            InterviewScheduleRepository scheduleRepository,
            CandidateRepository candidateRepository,
            DesignationRepository designationRepository,
            TechnologyRepository technologyRepository,
            NotificationService notificationService) {
        this.panelRepository = panelRepository;
        this.slotRepository = slotRepository;
        this.requestRepository = requestRepository;
        this.scheduleRepository = scheduleRepository;
        this.candidateRepository = candidateRepository;
        this.designationRepository = designationRepository;
        this.technologyRepository = technologyRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public InterviewPanelDto createPanelInterview(User requestedBy, CreatePanelInterviewDto dto) {
        logger.info("Creating panel interview: candidate='{}', {} interviewers",
                dto.candidateName(), dto.availabilitySlotIds().size());

        if (dto.availabilitySlotIds() == null || dto.availabilitySlotIds().isEmpty()) {
            throw new RuntimeException("At least one interviewer slot must be selected for a panel");
        }

        Candidate candidate = null;
        if (dto.candidateId() != null) {
            candidate = candidateRepository.findById(dto.candidateId())
                    .orElseThrow(() -> new RuntimeException("Candidate not found"));
        }
        String candidateName = candidate != null ? candidate.getName() : dto.candidateName();
        if (candidateName == null || candidateName.trim().isEmpty()) {
            throw new RuntimeException("Candidate name is required");
        }

        Designation designation = null;
        if (dto.candidateDesignationId() != null) {
            designation = designationRepository.findById(dto.candidateDesignationId())
                    .orElseThrow(() -> new RuntimeException("Designation not found"));
        }

        Set<Technology> technologies = new HashSet<>();
        if (dto.requiredTechnologyIds() != null && !dto.requiredTechnologyIds().isEmpty()) {
            technologies = new HashSet<>(technologyRepository.findAllById(dto.requiredTechnologyIds()));
        }

        // Validate all slots before creating anything
        List<AvailabilitySlot> slots = dto.availabilitySlotIds().stream()
                .map(slotId -> {
                    AvailabilitySlot slot = slotRepository.findById(slotId)
                            .orElseThrow(() -> new RuntimeException("Slot not found: " + slotId));
                    if (slot.getStatus() != SlotStatus.AVAILABLE) {
                        throw new RuntimeException("Slot for " + slot.getInterviewer().getFullName() + " is no longer available");
                    }
                    if (dto.startDateTime().isBefore(slot.getStartDateTime())) {
                        throw new RuntimeException("Panel start time is before " + slot.getInterviewer().getFullName() + "'s slot start");
                    }
                    if (dto.endDateTime().isAfter(slot.getEndDateTime())) {
                        throw new RuntimeException("Panel end time is after " + slot.getInterviewer().getFullName() + "'s slot end");
                    }
                    return slot;
                })
                .collect(Collectors.toList());

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

        Designation finalDesignation = designation;
        String finalCandidateName = candidateName;
        Set<Technology> finalTechnologies = technologies;

        for (AvailabilitySlot slot : slots) {
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
                    .availabilitySlot(bookedSlot)
                    .panel(panel)
                    .status(RequestStatus.ACCEPTED)
                    .respondedAt(LocalDateTime.now())
                    .isUrgent(dto.isUrgent())
                    .notes(dto.notes())
                    .responseNotes("Auto-accepted as part of panel interview")
                    .build();

            request = requestRepository.save(request);

            // Create InterviewSchedule and link back to slot
            InterviewSchedule schedule = InterviewSchedule.builder()
                    .request(request)
                    .interviewer(slot.getInterviewer())
                    .startDateTime(dto.startDateTime())
                    .endDateTime(dto.endDateTime())
                    .status(InterviewStatus.SCHEDULED)
                    .build();
            schedule = scheduleRepository.save(schedule);

            bookedSlot.setInterviewSchedule(schedule);
            slotRepository.save(bookedSlot);

            try {
                notificationService.sendInterviewScheduledNotification(request);
            } catch (Exception e) {
                logger.warn("Failed to send scheduled notification to {}: {}", slot.getInterviewer().getFullName(), e.getMessage());
            }
        }

        if (candidate != null) {
            candidate.setStatus(CandidateStatus.SCHEDULED);
            candidateRepository.save(candidate);
        }

        InterviewPanel savedPanel = panelRepository.findByIdWithDetails(panel.getId())
                .orElseThrow(() -> new RuntimeException("Panel not found after save"));
        return InterviewPanelDto.from(savedPanel);
    }

    /**
     * Cancel all requests in a panel and restore every booked slot to AVAILABLE.
     *
     * Same three-step fix as single-interview cancel:
     *  1. slot.setInterviewSchedule(null) — detach FK so HR calendar stops seeing it as booked
     *  2. slot.setStatus(AVAILABLE) + slot.setActive(true) — make it visible again
     *  3. Cancel the InterviewSchedule row
     */
    @Transactional
    public void cancelPanelInterview(User hrUser, Long panelId) {
        InterviewPanel panel = panelRepository.findByIdWithDetails(panelId)
                .orElseThrow(() -> new RuntimeException("Panel not found"));

        if (!panel.getRequestedBy().getId().equals(hrUser.getId())) {
            throw new RuntimeException("Unauthorized — you did not create this panel");
        }

        for (InterviewRequest request : panel.getPanelRequests()) {
            if (request.getStatus() == RequestStatus.CANCELLED) continue;

            // ── Restore the slot ────────────────────────────────────────────
            AvailabilitySlot slot = request.getAvailabilitySlot();
            if (slot != null) {
                logger.info("Panel cancel: restoring slot {} for interviewer {}",
                        slot.getId(), request.getAssignedInterviewer().getFullName());

                slot.setInterviewSchedule(null);   // detach schedule link
                slot.setStatus(SlotStatus.AVAILABLE);
                slot.setActive(true);              // ← THE FIX
                slot.setDescription(null);
                slotRepository.save(slot);
            }

            // ── Cancel the InterviewSchedule ────────────────────────────────
            scheduleRepository.findByRequestId(request.getId()).ifPresent(schedule -> {
                schedule.setStatus(InterviewStatus.CANCELLED);
                scheduleRepository.save(schedule);
            });

            // ── Cancel the request ──────────────────────────────────────────
            request.setStatus(RequestStatus.CANCELLED);
            request.setAvailabilitySlot(null);
            requestRepository.save(request);

            // ── Notify interviewer ──────────────────────────────────────────
            try {
                notificationService.sendInterviewCancelledNotification(request);
            } catch (Exception e) {
                logger.warn("Failed to send cancellation notification: {}", e.getMessage());
            }
        }

        // Reset candidate status
        if (panel.getCandidate() != null) {
            Candidate candidate = panel.getCandidate();
            long activeCount = requestRepository.findByCandidateId(candidate.getId())
                    .stream()
                    .filter(r -> r.getStatus() == RequestStatus.ACCEPTED)
                    .count();
            if (activeCount == 0) {
                candidate.setStatus(CandidateStatus.SCREENING);
                candidateRepository.save(candidate);
            }
        }

        logger.info("Cancelled panel {} — all slots restored", panelId);
    }

    public List<InterviewPanelDto> getPanelsByCandidateId(Long candidateId) {
        return panelRepository.findByCandidateId(candidateId)
                .stream().map(InterviewPanelDto::from).collect(Collectors.toList());
    }

    public List<InterviewPanelDto> getPanelsByRequestedBy(Long userId) {
        return panelRepository.findByRequestedById(userId)
                .stream().map(InterviewPanelDto::from).collect(Collectors.toList());
    }

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

        slot.setActive(false);
        slotRepository.save(slot);

        if (bookStart.isAfter(slotStart)) {
            slotRepository.save(AvailabilitySlot.builder()
                    .interviewer(slot.getInterviewer())
                    .startDateTime(slotStart).endDateTime(bookStart)
                    .description(slot.getDescription())
                    .status(SlotStatus.AVAILABLE).isActive(true).build());
        }

        AvailabilitySlot booked = AvailabilitySlot.builder()
                .interviewer(slot.getInterviewer())
                .startDateTime(bookStart).endDateTime(bookEnd)
                .description("Panel Interview: " + candidateName)
                .status(SlotStatus.BOOKED).isActive(true).build();
        booked = slotRepository.save(booked);

        if (bookEnd.isBefore(slotEnd)) {
            slotRepository.save(AvailabilitySlot.builder()
                    .interviewer(slot.getInterviewer())
                    .startDateTime(bookEnd).endDateTime(slotEnd)
                    .description(slot.getDescription())
                    .status(SlotStatus.AVAILABLE).isActive(true).build());
        }

        return booked;
    }
}