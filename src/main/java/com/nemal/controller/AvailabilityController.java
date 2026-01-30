package com.nemal.controller;

import com.nemal.dto.AvailabilitySlotDto;
import com.nemal.dto.BulkAvailabilitySlotDto;
import com.nemal.dto.CreateAvailabilitySlotDto;
import com.nemal.entity.User;
import com.nemal.service.AvailabilityService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/availability")
@CrossOrigin(origins = "http://localhost:5173")
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    public AvailabilityController(AvailabilityService availabilityService) {
        this.availabilityService = availabilityService;
    }

    @GetMapping
    public ResponseEntity<List<AvailabilitySlotDto>> getMyAvailability(
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(availabilityService.getInterviewerAvailability(user));
    }

    @GetMapping("/range")
    public ResponseEntity<List<AvailabilitySlotDto>> getAvailabilityByDateRange(
            @AuthenticationPrincipal User user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end
    ) {
        return ResponseEntity.ok(
                availabilityService.getInterviewerAvailabilityByDateRange(user, start, end)
        );
    }

    @PostMapping
    public ResponseEntity<AvailabilitySlotDto> createAvailabilitySlot(
            @AuthenticationPrincipal User user,
            @RequestBody CreateAvailabilitySlotDto dto
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(availabilityService.createAvailabilitySlot(user, dto));
    }

    @PostMapping("/bulk")
    public ResponseEntity<List<AvailabilitySlotDto>> createBulkAvailabilitySlots(
            @AuthenticationPrincipal User user,
            @RequestBody BulkAvailabilitySlotDto dto
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(availabilityService.createBulkAvailabilitySlots(user, dto));
    }

    @DeleteMapping("/{slotId}")
    public ResponseEntity<Void> deleteAvailabilitySlot(
            @AuthenticationPrincipal User user,
            @PathVariable Long slotId
    ) {
        availabilityService.deleteAvailabilitySlot(user, slotId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> getAvailabilityStats(
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(Map.of(
                "availableSlots", availabilityService.getAvailableSlotCount(user.getId()),
                "bookedSlots", availabilityService.getBookedSlotCount(user.getId())
        ));
    }
}