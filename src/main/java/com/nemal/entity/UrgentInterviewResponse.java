package com.nemal.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "urgent_interview_responses")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UrgentInterviewResponse {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private UrgentInterviewBroadcast broadcast;

    @ManyToOne
    private User interviewer;

    private boolean canAttend;

    private String proposedSlot;

    private String notes;
}
