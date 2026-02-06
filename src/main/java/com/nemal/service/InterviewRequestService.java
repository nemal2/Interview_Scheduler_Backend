// InterviewRequestService.java
package com.nemal.service;

import com.nemal.dto.CreateInterviewRequestDto;
import com.nemal.dto.InterviewRequestDto;
import com.nemal.entity.*;
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
public class InterviewRequestService {

    private static final Logger logger = LoggerFactory.getLogger(InterviewRequestService.class);

    private final InterviewRequestRepository interviewRequestRepository;
    private final AvailabilitySlotRepository availabilitySlotRepository;
    private final DesignationRepository designationRepository;
    private final TechnologyRepository technologyRepository;
    private final CandidateRepository candidateRepository;
    private final NotificationService notificationService;

    public InterviewRequestService(
            InterviewRequestRepository interviewRequestRepository,
            AvailabilitySlotRepository availabilitySlotRepository,
            DesignationRepository designationRepository,
            TechnologyRepository technologyRepository,
            CandidateRepository candidateRepository,
            NotificationService notificationService
    ) {
        this.interviewRequestRepository = interviewRequestRepository;
        this.availabilitySlotRepository = availabilitySlotRepository;
        this.designationRepository = designationRepository;
        this.technologyRepository = technologyRepository;
        this.candidateRepository = candidateRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public InterviewRequestDto createInterviewRequest(User requestedBy, CreateInterviewRequestDto dto) {
        logger.info("Creating interview request for candidate: {} on slot: {}",
                dto.candidateName(), dto.availabilitySlotId());

        // Get the availability slot
        AvailabilitySlot slot = availabilitySlotRepository.findById(dto.availabilitySlotId())
                .orElseThrow(() -> new RuntimeException("Availability slot not found"));

        // Verify slot is available
        if (slot.getStatus() != SlotStatus.AVAILABLE) {
            logger.error("Slot {} is not available, current status: {}", slot.getId(), slot.getStatus());
            throw new RuntimeException("This time slot is no longer available");
        }

        // Get designation if provided
        Designation designation = null;
        if (dto.candidateDesignationId() != null) {
            designation = designationRepository.findById(dto.candidateDesignationId())
                    .orElseThrow(() -> new RuntimeException("Designation not found"));
        }

        // Get technologies
        Set<Technology> technologies = new HashSet<>();
        if (dto.requiredTechnologyIds() != null && !dto.requiredTechnologyIds().isEmpty()) {
            technologies = new HashSet<>(technologyRepository.findAllById(dto.requiredTechnologyIds()));
            logger.info("Found {} technologies for the interview", technologies.size());
        }

        // Get or create candidate if candidateId is provided
        Candidate candidate = null;
        if (dto.candidateId() != null) {
            candidate = candidateRepository.findById(dto.candidateId())
                    .orElseThrow(() -> new RuntimeException("Candidate not found"));
        }

        // Create interview request - AUTOMATICALLY ACCEPTED
        InterviewRequest request = InterviewRequest.builder()
                .candidateName(dto.candidateName())
                .candidate(candidate)
                .candidateDesignation(designation)
                .requiredTechnologies(technologies)
                .preferredStartDateTime(dto.preferredStartDateTime())
                .preferredEndDateTime(dto.preferredEndDateTime())
                .requestedBy(requestedBy)
                .assignedInterviewer(slot.getInterviewer())
                .availabilitySlot(slot)
                .status(RequestStatus.ACCEPTED) // AUTO-ACCEPT
                .respondedAt(LocalDateTime.now()) // Set responded time immediately
                .isUrgent(dto.isUrgent())
                .notes(dto.notes())
                .responseNotes("Auto-accepted when scheduled by HR")
                .build();

        request = interviewRequestRepository.save(request);
        logger.info("Created and auto-accepted interview request with ID: {}", request.getId());

        // Mark slot as BOOKED immediately
        slot.setStatus(SlotStatus.BOOKED);
        availabilitySlotRepository.save(slot);
        logger.info("Marked slot {} as BOOKED", slot.getId());

        // Update candidate status if candidate exists
        if (candidate != null) {
            candidate.setStatus(com.nemal.enums.CandidateStatus.SCHEDULED);
            candidateRepository.save(candidate);
            logger.info("Updated candidate {} status to SCHEDULED", candidate.getId());
        }

        // Send notification to interviewer (informational only)
        try {
            notificationService.sendInterviewScheduledNotification(request);
        } catch (Exception e) {
            logger.error("Failed to send notification: {}", e.getMessage());
            // Don't fail the request if notification fails
        }

        return InterviewRequestDto.from(request);
    }

    @Transactional
    public void cancelInterviewRequest(User hrUser, Long requestId) {
        logger.info("HR user {} canceling request {}", hrUser.getId(), requestId);

        InterviewRequest request = interviewRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Interview request not found"));

        // Only HR who created the request can cancel
        if (!request.getRequestedBy().getId().equals(hrUser.getId())) {
            throw new RuntimeException("Unauthorized - you did not create this request");
        }

        // Can only cancel ACCEPTED requests (since we auto-accept now)
        if (request.getStatus() != RequestStatus.ACCEPTED) {
            throw new RuntimeException("Cannot cancel this request in its current status");
        }

        // Free up the slot
        AvailabilitySlot slot = request.getAvailabilitySlot();
        if (slot != null) {
            slot.setStatus(SlotStatus.AVAILABLE);
            slot.setInterviewSchedule(null);
            availabilitySlotRepository.save(slot);
            logger.info("Freed up slot {} after cancellation", slot.getId());
        }

        // Update candidate status if candidate exists
        if (request.getCandidate() != null) {
            request.getCandidate().setStatus(com.nemal.enums.CandidateStatus.SCREENING);
            candidateRepository.save(request.getCandidate());
        }

        // Update request status
        request.setStatus(RequestStatus.CANCELLED);
        interviewRequestRepository.save(request);
        logger.info("Cancelled interview request {}", requestId);

        // Notify interviewer about cancellation
        try {
            notificationService.sendInterviewCancelledNotification(request);
        } catch (Exception e) {
            logger.error("Failed to send cancellation notification: {}", e.getMessage());
        }
    }

    public List<InterviewRequestDto> getMyRequests(User interviewer) {
        return interviewRequestRepository.findByAssignedInterviewerId(interviewer.getId())
                .stream()
                .map(InterviewRequestDto::from)
                .collect(Collectors.toList());
    }

    public List<InterviewRequestDto> getUpcomingInterviews(User interviewer) {
        return interviewRequestRepository.findUpcomingInterviewsForInterviewer(interviewer.getId(), LocalDateTime.now())
                .stream()
                .map(InterviewRequestDto::from)
                .collect(Collectors.toList());
    }

    public long getUpcomingInterviewCount(User interviewer) {
        return interviewRequestRepository.countUpcomingInterviewsForInterviewer(interviewer.getId());
    }

    public List<InterviewRequestDto> getHRRequests(User hrUser) {
        return interviewRequestRepository.findByRequestedById(hrUser.getId())
                .stream()
                .map(InterviewRequestDto::from)
                .collect(Collectors.toList());
    }
}