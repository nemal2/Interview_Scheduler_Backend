// AvailabilityFilterDto.java
package com.nemal.dto;

import java.time.LocalDateTime;
import java.util.List;

public record AvailabilityFilterDto(
        List<Long> departmentIds,
        List<Long> technologyIds,
        LocalDateTime startDateTime,
        LocalDateTime endDateTime,
        Integer minYearsOfExperience
) {}