package com.nemal.dto;

import com.nemal.entity.Tier;

public record TierSimpleDto(
        Long id,
        String name,
        Integer tierOrder
) {
    public static TierSimpleDto from(Tier tier) {
        return new TierSimpleDto(
                tier.getId(),
                tier.getName(),
                tier.getTierOrder()
        );
    }
}