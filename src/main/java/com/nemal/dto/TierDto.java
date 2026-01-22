package com.nemal.dto;

import com.nemal.entity.Tier;

public record TierDto(
        Long id,
        String name,
        Long departmentId,
        String departmentName,
        Integer tierOrder,
        String description,
        boolean isActive
) {
    public static TierDto from(Tier tier) {
        return new TierDto(
                tier.getId(),
                tier.getName(),
                tier.getDepartment() != null ? tier.getDepartment().getId() : null,
                tier.getDepartment() != null ? tier.getDepartment().getName() : null,
                tier.getTierOrder(),
                tier.getDescription(),
                tier.isActive()
        );
    }
}