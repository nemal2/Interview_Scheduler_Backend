package com.nemal.dto;

import java.time.LocalDateTime;

public record CreateInterviewScheduleDto(
        Long interviewRequestId,
        Long candidateId,
        LocalDateTime startDateTime,
        LocalDateTime endDateTime,
        String meetingLink,
        String location,
        String notes
) {}