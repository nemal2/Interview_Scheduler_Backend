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

    // The candidate's name (can be freeform or from Candidate record)
    @Column(nullable = false)
    private String candidateName;

    // Optional link to Candidate record
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id")
    private Candidate candidate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_designation_id")
    private Designation candidateDesignation;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "interview_request_technologies",
            joinColumns = @JoinColumn(name = "interview_request_id"),
            inverseJoinColumns = @JoinColumn(name = "technology_id")
    )
    @Builder.Default
    private Set<Technology> requiredTechnologies = new HashSet<>();

    @Column(nullable = false)
    private LocalDateTime preferredStartDateTime;

    @Column(nullable = false)
    private LocalDateTime preferredEndDateTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by_id", nullable = false)
    private User requestedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_interviewer_id")
    private User assignedInterviewer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "availability_slot_id")
    private AvailabilitySlot availabilitySlot;

    // If this request is part of a panel interview
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "panel_id")
    private InterviewPanel panel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status = RequestStatus.PENDING;

    private LocalDateTime respondedAt;

    @Column(length = 2000)
    private String responseNotes;

    @Column(nullable = false)
    private boolean isUrgent = false;

    @Column(length = 2000)
    private String notes;


    // REPLACE with (if you want to navigate from request → schedule):
    @OneToOne(mappedBy = "request", fetch = FetchType.LAZY)
    private InterviewSchedule interviewSchedule;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public boolean isUrgent() {
        return isUrgent;
    }

    public void setUrgent(boolean urgent) {
        isUrgent = urgent;
    }
}