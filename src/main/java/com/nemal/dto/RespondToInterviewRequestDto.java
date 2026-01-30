// RespondToInterviewRequestDto.java
package com.nemal.dto;

public record RespondToInterviewRequestDto(
        boolean accepted,
        String responseNotes
) {}