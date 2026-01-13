package com.nemal.dto;

import com.nemal.entity.Department;

public record DepartmentSimpleDto(Long id, String name, String code) {
    public static DepartmentSimpleDto from(Department dept) {
        return new DepartmentSimpleDto(dept.getId(), dept.getName(), dept.getCode());
    }
}