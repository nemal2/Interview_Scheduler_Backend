package com.nemal.dto;

public record UpdateTierDto(
        String name,
        Integer tierOrder,
        String description,
        Boolean isActive
) {}