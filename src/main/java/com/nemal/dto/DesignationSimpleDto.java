package com.nemal.dto;

import com.nemal.entity.Designation;

public record DesignationSimpleDto(Long id, String name, Integer hierarchyLevel) {
    public static DesignationSimpleDto from(Designation des) {
        return new DesignationSimpleDto(des.getId(), des.getName(), des.getHierarchyLevel());
    }
}