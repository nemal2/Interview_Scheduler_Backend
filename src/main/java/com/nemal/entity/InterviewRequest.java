package com.nemal.entity;

import com.nemal.enums.RequestStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "interview_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String candidateName;

    @ManyToOne
    private Designation candidateDesignation;

    @ManyToMany
    @JoinTable(name = "interview_requests_technologies",
            joinColumns = @JoinColumn(name = "interview_request_id"),
            inverseJoinColumns = @JoinColumn(name = "technology_id"))
    private Set<Technology> requiredTechnologies = new HashSet<>();

    private LocalDate preferredDate;

    private LocalTime preferredStart;

    private LocalTime preferredEnd;

    @ManyToOne
    private User requestedBy;

    @ManyToOne
    private User assignedInterviewer;

    @Enumerated(EnumType.STRING)
    private RequestStatus status = RequestStatus.PENDING;

    private boolean isUrgent = false;

    private String notes;

    private LocalDateTime deadline;

    @CreatedDate
    private LocalDateTime createdAt;

    private LocalDateTime respondedAt;
}
