package com.nemal.dto;

public record CreateDesignationDto(
        String name,
        Integer hierarchyLevel,
        Long departmentId,
        String description
) {}
