package com.nemal.dto;

import com.nemal.entity.Designation;

public record DesignationDto(
        Long id,
        String name,
        Integer levelOrder,
        Long departmentId,
        String departmentName,
        Long tierId,
        String tierName,
        Integer tierOrder,
        String description,
        boolean isActive
) {
    public static DesignationDto from(Designation des) {
        return new DesignationDto(
                des.getId(),
                des.getName(),
                des.getLevelOrder(),
                des.getDepartment() != null ? des.getDepartment().getId() : null,
                des.getDepartment() != null ? des.getDepartment().getName() : null,
                des.getTier() != null ? des.getTier().getId() : null,
                des.getTier() != null ? des.getTier().getName() : null,
                des.getTier() != null ? des.getTier().getTierOrder() : null,
                des.getDescription(),
                des.isActive()
        );
    }
}