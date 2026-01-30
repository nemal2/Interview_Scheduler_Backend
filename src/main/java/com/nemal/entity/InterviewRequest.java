// Update InterviewRequest.java
package com.nemal.entity;

import com.nemal.enums.RequestStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "interview_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class InterviewRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String candidateName;

    @ManyToOne
    @JoinColumn(name = "candidate_designation_id")
    private Designation candidateDesignation;

    @ManyToMany
    @JoinTable(
            name = "interview_requests_technologies",
            joinColumns = @JoinColumn(name = "interview_request_id"),
            inverseJoinColumns = @JoinColumn(name = "technology_id")
    )
    private Set<Technology> requiredTechnologies = new HashSet<>();

    @Column(nullable = false)
    private LocalDateTime preferredStartDateTime;

    @Column(nullable = false)
    private LocalDateTime preferredEndDateTime;

    @ManyToOne
    @JoinColumn(name = "requested_by_id", nullable = false)
    private User requestedBy;

    @ManyToOne
    @JoinColumn(name = "assigned_interviewer_id")
    private User assignedInterviewer;

    @OneToOne
    @JoinColumn(name = "availability_slot_id")
    private AvailabilitySlot availabilitySlot;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status = RequestStatus.PENDING;

    @Column(nullable = false)
    private boolean isUrgent = false;

    @Column(length = 1000)
    private String notes;

    @Column(length = 1000)
    private String responseNotes;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    private LocalDateTime respondedAt;
}