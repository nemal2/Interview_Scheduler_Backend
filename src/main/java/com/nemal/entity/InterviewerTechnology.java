package com.nemal.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "interviewer_technologies")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewerTechnology {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private User interviewer;

    @ManyToOne
    private Technology technology;

    private int yearsOfExperience;

    private boolean isActive = true;
}