package com.nemal.service;

import com.nemal.dto.CreateInterviewRequestDto;
import com.nemal.dto.InterviewRequestDto;
import com.nemal.entity.*;
import com.nemal.enums.CandidateStatus;
import com.nemal.enums.InterviewStatus;
import com.nemal.enums.RequestStatus;
import com.nemal.enums.SlotStatus;
import com.nemal.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InterviewRequestService {

    private final InterviewRequestRepository interviewRequestRepository;
    private final AvailabilitySlotRepository availabilitySlotRepository;
    private final InterviewScheduleRepository interviewScheduleRepository;
    private final UserRepository userRepository;
    private final CandidateRepository candidateRepository;
    private final DesignationRepository designationRepository;
    private final TechnologyRepository technologyRepository;

    @Transactional
    public InterviewRequestDto createInterviewRequest(User requestedBy, CreateInterviewRequestDto dto) {
        // 1. Validate and fetch the slot
        AvailabilitySlot slot = availabilitySlotRepository.findById(dto.availabilitySlotId())
                .orElseThrow(() -> new RuntimeException("Availability slot not found: " + dto.availabilitySlotId()));

        if (slot.getStatus() != SlotStatus.AVAILABLE) {
            throw new RuntimeException("Slot is not available");
        }

        // 2. Determine booking times (use slot times if not specified)
        LocalDateTime bookingStart = dto.preferredStartDateTime() != null
                ? dto.preferredStartDateTime() : slot.getStartDateTime();
        LocalDateTime bookingEnd = dto.preferredEndDateTime() != null
                ? dto.preferredEndDateTime() : slot.getEndDateTime();

        // 3. Validate booking fits within slot
        if (bookingStart.isBefore(slot.getStartDateTime().minusSeconds(1)) ||
                bookingEnd.isAfter(slot.getEndDateTime().plusSeconds(1))) {
            throw new RuntimeException("Booking time must be within the slot's available time: " +
                    "booking=[" + bookingStart + " - " + bookingEnd + "] " +
                    "slot=[" + slot.getStartDateTime() + " - " + slot.getEndDateTime() + "]");
        }

        // 4. Fetch candidate
        Candidate candidate = null;
        if (dto.candidateId() != null) {
            candidate = candidateRepository.findById(dto.candidateId())
                    .orElseThrow(() -> new RuntimeException("Candidate not found: " + dto.candidateId()));
        }

        String candidateName = dto.candidateName() != null ? dto.candidateName()
                : (candidate != null ? candidate.getName() : "Unknown");

        // 5. Fetch designation
        Designation candidateDesignation = null;
        if (dto.candidateDesignationId() != null) {
            candidateDesignation = designationRepository.findById(dto.candidateDesignationId())
                    .orElseThrow(() -> new RuntimeException("Designation not found"));
        }

        // 6. Fetch technologies
        List<Technology> technologies = dto.requiredTechnologyIds() != null
                ? technologyRepository.findAllById(dto.requiredTechnologyIds())
                : List.of();

        // 7. Build the interview request — auto-accepted, no interviewer approval needed
        InterviewRequest request = InterviewRequest.builder()
                .candidateName(candidateName)
                .candidate(candidate)
                .candidateDesignation(candidateDesignation)
                .preferredStartDateTime(bookingStart)
                .preferredEndDateTime(bookingEnd)
                .requestedBy(requestedBy)
                .assignedInterviewer(slot.getInterviewer())
                .availabilitySlot(slot)
                .status(RequestStatus.ACCEPTED)
                .respondedAt(LocalDateTime.now())
                .responseNotes("Auto-accepted by HR scheduling")
                .isUrgent(dto.isUrgent())
                .notes(dto.notes())
                .build();

        request.getRequiredTechnologies().addAll(technologies);

        // 8. Handle slot splitting
        boolean isPartialBooking = !bookingStart.equals(slot.getStartDateTime())
                || !bookingEnd.equals(slot.getEndDateTime());

        if (isPartialBooking) {
            slot.setStatus(SlotStatus.BOOKED);
            slot.setActive(false);
            availabilitySlotRepository.save(slot);

            if (bookingStart.isAfter(slot.getStartDateTime())) {
                AvailabilitySlot beforeSlot = AvailabilitySlot.builder()
                        .interviewer(slot.getInterviewer())
                        .startDateTime(slot.getStartDateTime())
                        .endDateTime(bookingStart)
                        .status(SlotStatus.AVAILABLE)
                        .description(slot.getDescription())
                        .isActive(true)
                        .build();
                availabilitySlotRepository.save(beforeSlot);
            }

            if (bookingEnd.isBefore(slot.getEndDateTime())) {
                AvailabilitySlot afterSlot = AvailabilitySlot.builder()
                        .interviewer(slot.getInterviewer())
                        .startDateTime(bookingEnd)
                        .endDateTime(slot.getEndDateTime())
                        .status(SlotStatus.AVAILABLE)
                        .description(slot.getDescription())
                        .isActive(true)
                        .build();
                availabilitySlotRepository.save(afterSlot);
            }
        } else {
            slot.setStatus(SlotStatus.BOOKED);
            availabilitySlotRepository.save(slot);
        }

        // 9. Save request
        InterviewRequest saved = interviewRequestRepository.save(request);

        // 10. Auto-create InterviewSchedule
        InterviewSchedule schedule = InterviewSchedule.builder()
                .request(saved)
                .interviewer(slot.getInterviewer())
                .startDateTime(bookingStart)
                .endDateTime(bookingEnd)
                .status(InterviewStatus.SCHEDULED)
                .build();
        interviewScheduleRepository.save(schedule);

        // 11. Update candidate status
        if (candidate != null) {
            candidate.setStatus(CandidateStatus.SCHEDULED);
            candidateRepository.save(candidate);
        }

        return InterviewRequestDto.from(saved);
    }

    public List<InterviewRequestDto> getRequestsByUser(Long userId) {
        return interviewRequestRepository.findByRequestedById(userId)
                .stream().map(InterviewRequestDto::from).collect(Collectors.toList());
    }

    public List<InterviewRequestDto> getRequestsByCandidate(Long candidateId) {
        return interviewRequestRepository.findByCandidateId(candidateId)
                .stream().map(InterviewRequestDto::from).collect(Collectors.toList());
    }

    public List<InterviewRequestDto> getUpcomingInterviewsForInterviewer(Long interviewerId) {
        return interviewRequestRepository.findUpcomingInterviewsForInterviewer(interviewerId, LocalDateTime.now())
                .stream().map(InterviewRequestDto::from).collect(Collectors.toList());
    }

    public List<InterviewRequestDto> getInterviewsForInterviewer(Long interviewerId) {
        return interviewRequestRepository.findByAssignedInterviewerId(interviewerId)
                .stream().map(InterviewRequestDto::from).collect(Collectors.toList());
    }

    @Transactional
    public InterviewRequestDto respondToRequest(User interviewer, Long requestId, String action, String notes) {
        InterviewRequest request = interviewRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found: " + requestId));

        if (!request.getAssignedInterviewer().getId().equals(interviewer.getId())) {
            throw new RuntimeException("You are not assigned to this request");
        }

        if ("ACCEPT".equalsIgnoreCase(action)) {
            request.setStatus(RequestStatus.ACCEPTED);
        } else if ("DECLINE".equalsIgnoreCase(action)) {
            request.setStatus(RequestStatus.REJECTED);
            if (request.getAvailabilitySlot() != null) {
                AvailabilitySlot slot = request.getAvailabilitySlot();
                slot.setStatus(SlotStatus.AVAILABLE);
                availabilitySlotRepository.save(slot);
            }
        } else {
            throw new RuntimeException("Invalid action: " + action);
        }

        request.setRespondedAt(LocalDateTime.now());
        request.setResponseNotes(notes);
        return InterviewRequestDto.from(interviewRequestRepository.save(request));
    }

    @Transactional
    public void cancelRequest(User user, Long requestId) {
        InterviewRequest request = interviewRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found: " + requestId));

        if (request.getAvailabilitySlot() != null) {
            AvailabilitySlot slot = request.getAvailabilitySlot();
            slot.setStatus(SlotStatus.AVAILABLE);
            availabilitySlotRepository.save(slot);
        }

        request.setStatus(RequestStatus.CANCELLED);
        interviewRequestRepository.save(request);
    }
}