package com.nemal.dto;

import com.nemal.entity.Designation;

public record DesignationSimpleDto(
        Long id,
        String name,
        Integer levelOrder,
        String tierName
) {
    public static DesignationSimpleDto from(Designation des) {
        return new DesignationSimpleDto(
                des.getId(),
                des.getName(),
                des.getLevelOrder(),
                des.getTier() != null ? des.getTier().getName() : null
        );
    }
}