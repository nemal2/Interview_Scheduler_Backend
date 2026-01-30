package com.nemal.controller;

import com.nemal.dto.AvailabilityFilterDto;
import com.nemal.dto.InterviewerAvailabilityDto;
import com.nemal.service.HRAvailabilityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/hr/availability")
@CrossOrigin(origins = "http://localhost:5173")
public class HRAvailabilityController {

    private static final Logger logger = LoggerFactory.getLogger(HRAvailabilityController.class);
    private final HRAvailabilityService hrAvailabilityService;

    public HRAvailabilityController(HRAvailabilityService hrAvailabilityService) {
        this.hrAvailabilityService = hrAvailabilityService;
    }

    @PostMapping("/filter")
    public ResponseEntity<?> getFilteredAvailability(
            @RequestBody(required = false) AvailabilityFilterDto filter
    ) {
        try {
            logger.info("Received filter request: {}", filter);
            List<InterviewerAvailabilityDto> result = hrAvailabilityService.getAllAvailableSlots(filter);
            logger.info("Returning {} availability slots", result.size());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error in getFilteredAvailability: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Failed to fetch availability",
                            "message", e.getMessage(),
                            "timestamp", System.currentTimeMillis()
                    ));
        }
    }

    @GetMapping
    public ResponseEntity<?> getAllAvailability() {
        try {
            logger.info("Received request for all availability");
            List<InterviewerAvailabilityDto> result = hrAvailabilityService.getAllAvailableSlots(null);
            logger.info("Returning {} availability slots", result.size());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error in getAllAvailability: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Failed to fetch availability",
                            "message", e.getMessage(),
                            "timestamp", System.currentTimeMillis()
                    ));
        }
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleException(Exception e) {
        logger.error("Unhandled exception in HRAvailabilityController: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "error", "Internal server error",
                        "message", e.getMessage(),
                        "timestamp", System.currentTimeMillis()
                ));
    }
}