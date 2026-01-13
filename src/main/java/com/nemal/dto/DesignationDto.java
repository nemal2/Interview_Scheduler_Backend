package com.nemal.dto;

import com.nemal.entity.Designation;

public record DesignationDto(Long id, String name, Integer hierarchyLevel) {
    public static DesignationDto from(Designation des) {
        return new DesignationDto(des.getId(), des.getName(), des.getHierarchyLevel());
    }
}