package com.nemal.dto;

import com.nemal.entity.Technology;

public record TechnologyDto(Long id, String name, String category, boolean isActive) {
    public static TechnologyDto from(Technology tech) {
        return new TechnologyDto(tech.getId(), tech.getName(), tech.getCategory(), tech.isActive());
    }
}