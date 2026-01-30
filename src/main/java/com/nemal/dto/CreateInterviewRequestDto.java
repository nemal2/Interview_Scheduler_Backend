// CreateInterviewRequestDto.java
package com.nemal.dto;

import java.time.LocalDateTime;
import java.util.List;

public record CreateInterviewRequestDto(
        String candidateName,
        Long candidateDesignationId,
        List<Long> requiredTechnologyIds,
        Long availabilitySlotId,
        LocalDateTime preferredStartDateTime,
        LocalDateTime preferredEndDateTime,
        boolean isUrgent,
        String notes
) {}