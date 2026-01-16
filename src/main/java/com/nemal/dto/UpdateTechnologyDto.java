package com.nemal.dto;

public record UpdateTechnologyDto(
        String name,
        String category,
        Boolean isActive
) {}