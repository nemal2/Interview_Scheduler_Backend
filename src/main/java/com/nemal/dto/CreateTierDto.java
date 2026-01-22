package com.nemal.dto;

public record CreateTierDto(
        String name,
        Long departmentId,
        Integer tierOrder,
        String description
) {}