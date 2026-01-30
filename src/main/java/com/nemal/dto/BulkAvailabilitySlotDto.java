package com.nemal.dto;

import java.time.LocalDateTime;
import java.util.List;

public record BulkAvailabilitySlotDto(
        List<CreateAvailabilitySlotDto> slots
) {}