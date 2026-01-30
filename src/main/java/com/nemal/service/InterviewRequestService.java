// InterviewRequestService.java
package com.nemal.service;

import com.nemal.dto.CreateInterviewRequestDto;
import com.nemal.dto.InterviewRequestDto;
import com.nemal.dto.RespondToInterviewRequestDto;
import com.nemal.entity.*;
import com.nemal.enums.RequestStatus;
import com.nemal.enums.SlotStatus;
import com.nemal.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class InterviewRequestService {

    private final InterviewRequestRepository interviewRequestRepository;
    private final AvailabilitySlotRepository availabilitySlotRepository;
    private final DesignationRepository designationRepository;
    private final TechnologyRepository technologyRepository;
    private final NotificationService notificationService;

    public InterviewRequestService(
            InterviewRequestRepository interviewRequestRepository,
            AvailabilitySlotRepository availabilitySlotRepository,
            DesignationRepository designationRepository,
            TechnologyRepository technologyRepository,
            NotificationService notificationService
    ) {
        this.interviewRequestRepository = interviewRequestRepository;
        this.availabilitySlotRepository = availabilitySlotRepository;
        this.designationRepository = designationRepository;
        this.technologyRepository = technologyRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public InterviewRequestDto createInterviewRequest(User requestedBy, CreateInterviewRequestDto dto) {
        // Get the availability slot
        AvailabilitySlot slot = availabilitySlotRepository.findById(dto.availabilitySlotId())
                .orElseThrow(() -> new RuntimeException("Availability slot not found"));

        // Verify slot is available
        if (slot.getStatus() != SlotStatus.AVAILABLE) {
            throw new RuntimeException("This time slot is no longer available");
        }

        // Get designation
        Designation designation = null;
        if (dto.candidateDesignationId() != null) {
            designation = designationRepository.findById(dto.candidateDesignationId())
                    .orElseThrow(() -> new RuntimeException("Designation not found"));
        }

        // Get technologies
        Set<Technology> technologies = new HashSet<>();
        if (dto.requiredTechnologyIds() != null && !dto.requiredTechnologyIds().isEmpty()) {
            technologies = new HashSet<>(technologyRepository.findAllById(dto.requiredTechnologyIds()));
        }

        // Create interview request
        InterviewRequest request = InterviewRequest.builder()
                .candidateName(dto.candidateName())
                .candidateDesignation(designation)
                .requiredTechnologies(technologies)
                .preferredStartDateTime(dto.preferredStartDateTime())
                .preferredEndDateTime(dto.preferredEndDateTime())
                .requestedBy(requestedBy)
                .assignedInterviewer(slot.getInterviewer())
                .availabilitySlot(slot)
                .status(RequestStatus.PENDING)
                .isUrgent(dto.isUrgent())
                .notes(dto.notes())
                .build();

        request = interviewRequestRepository.save(request);

        // Update slot status to BOOKED temporarily (will be finalized when accepted)
        slot.setStatus(SlotStatus.BOOKED);
        slot.setInterviewSchedule(null); // Will be set when request is accepted
        availabilitySlotRepository.save(slot);

        // Send notification to interviewer
        notificationService.sendInterviewRequestNotification(request);

        return InterviewRequestDto.from(request);
    }

    @Transactional
    public InterviewRequestDto respondToInterviewRequest(
            User interviewer,
            Long requestId,
            RespondToInterviewRequestDto dto
    ) {
        InterviewRequest request = interviewRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Interview request not found"));

        // Verify this request is for the interviewer
        if (!request.getAssignedInterviewer().getId().equals(interviewer.getId())) {
            throw new RuntimeException("Unauthorized");
        }

        // Verify request is still pending
        if (request.getStatus() != RequestStatus.PENDING) {
            throw new RuntimeException("This request has already been responded to");
        }

        request.setRespondedAt(LocalDateTime.now());
        request.setResponseNotes(dto.responseNotes());

        if (dto.accepted()) {
            request.setStatus(RequestStatus.ACCEPTED);
            // Slot remains BOOKED
        } else {
            request.setStatus(RequestStatus.REJECTED);
            // Free up the slot
            AvailabilitySlot slot = request.getAvailabilitySlot();
            if (slot != null) {
                slot.setStatus(SlotStatus.AVAILABLE);
                availabilitySlotRepository.save(slot);
            }
        }

        request = interviewRequestRepository.save(request);

        // Notify HR
        notificationService.sendInterviewRequestResponseNotification(request);

        return InterviewRequestDto.from(request);
    }

    public List<InterviewRequestDto> getMyRequests(User interviewer) {
        return interviewRequestRepository.findByAssignedInterviewerId(interviewer.getId())
                .stream()
                .map(InterviewRequestDto::from)
                .collect(Collectors.toList());
    }

    public List<InterviewRequestDto> getMyPendingRequests(User interviewer) {
        return interviewRequestRepository.findPendingRequestsForInterviewer(interviewer.getId())
                .stream()
                .map(InterviewRequestDto::from)
                .collect(Collectors.toList());
    }

    public long getPendingRequestCount(User interviewer) {
        return interviewRequestRepository.countPendingRequestsForInterviewer(interviewer.getId());
    }
}