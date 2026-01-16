package com.nemal.dto;

import com.nemal.entity.Designation;

public record DesignationDto(
        Long id,
        String name,
        Integer hierarchyLevel,
        Long departmentId,
        String departmentName,
        String description,
        boolean isActive
) {
    public static DesignationDto from(Designation designation) {
        return new DesignationDto(
                designation.getId(),
                designation.getName(),
                designation.getHierarchyLevel(),
                designation.getDepartment() != null ? designation.getDepartment().getId() : null,
                designation.getDepartment() != null ? designation.getDepartment().getName() : null,
                designation.getDescription(),
                designation.isActive()
        );
    }
}