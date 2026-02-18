package com.nemal.controller;

import com.nemal.dto.CreateInterviewRequestDto;
import com.nemal.dto.InterviewRequestDto;
import com.nemal.entity.User;
import com.nemal.service.InterviewRequestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/hr/interviews")
@CrossOrigin(origins = "http://localhost:5173")
public class InterviewRequestController {

    private static final Logger logger = LoggerFactory.getLogger(InterviewRequestController.class);
    private final InterviewRequestService interviewRequestService;

    public InterviewRequestController(InterviewRequestService interviewRequestService) {
        this.interviewRequestService = interviewRequestService;
    }

    /**
     * HR schedules a single-interviewer interview.
     * Supports partial slot booking (slot splitting).
     */
    @PostMapping
    public ResponseEntity<?> createInterviewRequest(
            @AuthenticationPrincipal User user,
            @RequestBody CreateInterviewRequestDto dto) {
        try {
            InterviewRequestDto result = interviewRequestService.createInterviewRequest(user, dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (Exception e) {
            logger.error("Failed to create interview request: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/my-requests")
    public ResponseEntity<?> getMyRequests(@AuthenticationPrincipal User user) {
        try {
            List<InterviewRequestDto> result = interviewRequestService.getRequestsByUser(user.getId());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Failed to get requests for user {}: {}", user.getId(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/candidate/{candidateId}")
    public ResponseEntity<?> getRequestsByCandidate(@PathVariable Long candidateId) {
        try {
            List<InterviewRequestDto> result = interviewRequestService.getRequestsByCandidate(candidateId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Failed to get requests for candidate {}: {}", candidateId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/{requestId}")
    public ResponseEntity<?> cancelRequest(
            @AuthenticationPrincipal User user,
            @PathVariable Long requestId) {
        try {
            interviewRequestService.cancelRequest(user, requestId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            logger.error("Failed to cancel request {}: {}", requestId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        }
    }
}