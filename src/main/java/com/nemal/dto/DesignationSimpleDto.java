package com.nemal.dto;

import com.nemal.entity.Designation;

public record DesignationSimpleDto(
        Long id,
        String name,
        Integer levelOrder,
        TierSimpleDto tier
) {
    public static DesignationSimpleDto from(Designation designation) {
        return new DesignationSimpleDto(
                designation.getId(),
                designation.getName(),
                designation.getLevelOrder(),
                designation.getTier() != null ? TierSimpleDto.from(designation.getTier()) : null
        );
    }
}