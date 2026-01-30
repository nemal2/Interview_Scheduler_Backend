// InterviewRequestController.java
package com.nemal.controller;

import com.nemal.dto.CreateInterviewRequestDto;
import com.nemal.dto.InterviewRequestDto;
import com.nemal.dto.RespondToInterviewRequestDto;
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

    @PostMapping
    public ResponseEntity<InterviewRequestDto> createInterviewRequest(
            @AuthenticationPrincipal User user,
            @RequestBody CreateInterviewRequestDto dto
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(interviewRequestService.createInterviewRequest(user, dto));
    }

    @GetMapping("/my-requests")
    public ResponseEntity<List<InterviewRequestDto>> getMyRequests(
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(interviewRequestService.getMyRequests(user));
    }

    @GetMapping("/pending")
    public ResponseEntity<List<InterviewRequestDto>> getMyPendingRequests(
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(interviewRequestService.getMyPendingRequests(user));
    }

    @PostMapping("/{requestId}/respond")
    public ResponseEntity<InterviewRequestDto> respondToRequest(
            @AuthenticationPrincipal User user,
            @PathVariable Long requestId,
            @RequestBody RespondToInterviewRequestDto dto
    ) {
        return ResponseEntity.ok(interviewRequestService.respondToInterviewRequest(user, requestId, dto));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> getRequestStats(
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(Map.of(
                "pendingRequests", interviewRequestService.getPendingRequestCount(user)
        ));
    }
}