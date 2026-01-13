package com.nemal.dto;

import com.nemal.entity.Department;

public record DepartmentDto(Long id, String name, String code) {
    public static DepartmentDto from(Department dept) {
        return new DepartmentDto(dept.getId(), dept.getName(), dept.getCode());
    }
}