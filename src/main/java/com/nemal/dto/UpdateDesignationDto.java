package com.nemal.dto;

public record UpdateDesignationDto(
        String name,
        Integer hierarchyLevel,
        String description,
        Boolean isActive
) {}