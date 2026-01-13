package com.nemal.dto;

import com.nemal.entity.InterviewerTechnology;

public record InterviewerTechnologyDto(
        Long id,
        TechnologyDto technology,
        int yearsOfExperience,
        boolean isActive
) {
    public static InterviewerTechnologyDto from(InterviewerTechnology it) {
        return new InterviewerTechnologyDto(
                it.getId(),
                TechnologyDto.from(it.getTechnology()),
                it.getYearsOfExperience(),
                it.isActive()
        );
    }
}