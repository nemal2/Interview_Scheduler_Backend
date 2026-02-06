// InterviewRequestController.java
package com.nemal.controller;

import com.nemal.dto.CreateInterviewRequestDto;
import com.nemal.dto.InterviewRequestDto;
import com.nemal.entity.User;
import com.nemal.service.InterviewRequestService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/interview-requests")
@CrossOrigin(origins = "http://localhost:5173")
public class InterviewRequestController {

    private final InterviewRequestService interviewRequestService;

    public InterviewRequestController(InterviewRequestService interviewRequestService) {
        this.interviewRequestService = interviewRequestService;
    }

    /**
     * Create interview request (auto-accepted)
     */
    @PostMapping
    public ResponseEntity<InterviewRequestDto> createInterviewRequest(
            @AuthenticationPrincipal User user,
            @RequestBody CreateInterviewRequestDto dto
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(interviewRequestService.createInterviewRequest(user, dto));
    }

    /**
     * Get interviewer's scheduled interviews
     */
    @GetMapping("/my-interviews")
    public ResponseEntity<List<InterviewRequestDto>> getMyInterviews(
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(interviewRequestService.getMyRequests(user));
    }

    /**
     * Get upcoming interviews for interviewer
     */
    @GetMapping("/upcoming")
    public ResponseEntity<List<InterviewRequestDto>> getUpcomingInterviews(
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(interviewRequestService.getUpcomingInterviews(user));
    }

    /**
     * Get HR's created interview requests
     */
    @GetMapping("/hr-requests")
    public ResponseEntity<List<InterviewRequestDto>> getHRRequests(
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(interviewRequestService.getHRRequests(user));
    }

    /**
     * Cancel interview request (HR only)
     */
    @DeleteMapping("/{requestId}/cancel")
    public ResponseEntity<Void> cancelRequest(
            @AuthenticationPrincipal User user,
            @PathVariable Long requestId
    ) {
        interviewRequestService.cancelInterviewRequest(user, requestId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get interview statistics for interviewer
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> getRequestStats(
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(Map.of(
                "upcomingInterviews", interviewRequestService.getUpcomingInterviewCount(user)
        ));
    }
}