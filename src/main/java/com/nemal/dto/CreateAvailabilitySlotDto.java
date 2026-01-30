package com.nemal.dto;

import java.time.LocalDateTime;

public record CreateAvailabilitySlotDto(
        LocalDateTime startDateTime,
        LocalDateTime endDateTime,
        String description
) {}