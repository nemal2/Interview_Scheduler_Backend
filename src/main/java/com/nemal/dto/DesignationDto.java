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
    public static DesignationDto from(Designation designation) {
        return new DesignationDto(
                designation.getId(),
                designation.getName(),
                designation.getLevelOrder(),
                designation.getDepartment() != null ? designation.getDepartment().getId() : null,
                designation.getDepartment() != null ? designation.getDepartment().getName() : null,
                designation.getTier() != null ? designation.getTier().getId() : null,
                designation.getTier() != null ? designation.getTier().getName() : null,
                designation.getTier() != null ? designation.getTier().getTierOrder() : null,
                designation.getDescription(),
                designation.isActive()
        );
    }
}