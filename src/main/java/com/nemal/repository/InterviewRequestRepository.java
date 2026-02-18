package com.nemal.repository;

import com.nemal.entity.InterviewRequest;
import com.nemal.enums.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface InterviewRequestRepository extends JpaRepository<InterviewRequest, Long> {

    @Query("SELECT DISTINCT r FROM InterviewRequest r " +
            "LEFT JOIN FETCH r.requiredTechnologies " +
            "LEFT JOIN FETCH r.assignedInterviewer " +
            "LEFT JOIN FETCH r.candidate " +
            "WHERE r.assignedInterviewer.id = :interviewerId " +
            "ORDER BY r.preferredStartDateTime DESC")
    List<InterviewRequest> findByAssignedInterviewerId(@Param("interviewerId") Long interviewerId);

    @Query("SELECT DISTINCT r FROM InterviewRequest r " +
            "LEFT JOIN FETCH r.requiredTechnologies " +
            "LEFT JOIN FETCH r.assignedInterviewer " +
            "LEFT JOIN FETCH r.candidate " +
            "WHERE r.assignedInterviewer.id = :interviewerId " +
            "AND r.preferredStartDateTime >= :now " +
            "AND r.status = 'ACCEPTED' " +
            "ORDER BY r.preferredStartDateTime ASC")
    List<InterviewRequest> findUpcomingInterviewsForInterviewer(
            @Param("interviewerId") Long interviewerId,
            @Param("now") LocalDateTime now);

    @Query("SELECT COUNT(r) FROM InterviewRequest r " +
            "WHERE r.assignedInterviewer.id = :interviewerId " +
            "AND r.preferredStartDateTime >= CURRENT_TIMESTAMP " +
            "AND r.status = 'ACCEPTED'")
    long countUpcomingInterviewsForInterviewer(@Param("interviewerId") Long interviewerId);

    @Query("SELECT DISTINCT r FROM InterviewRequest r " +
            "LEFT JOIN FETCH r.requiredTechnologies " +
            "LEFT JOIN FETCH r.assignedInterviewer " +
            "LEFT JOIN FETCH r.candidate " +
            "WHERE r.requestedBy.id = :userId " +
            "ORDER BY r.createdAt DESC")
    List<InterviewRequest> findByRequestedById(@Param("userId") Long userId);

    @Query("SELECT DISTINCT r FROM InterviewRequest r " +
            "LEFT JOIN FETCH r.requiredTechnologies " +
            "LEFT JOIN FETCH r.assignedInterviewer " +
            "LEFT JOIN FETCH r.candidate " +
            "WHERE r.candidate.id = :candidateId " +
            "ORDER BY r.preferredStartDateTime DESC")
    List<InterviewRequest> findByCandidateId(@Param("candidateId") Long candidateId);

    @Query("SELECT DISTINCT r FROM InterviewRequest r " +
            "LEFT JOIN FETCH r.requiredTechnologies " +
            "LEFT JOIN FETCH r.assignedInterviewer " +
            "LEFT JOIN FETCH r.candidate " +
            "WHERE r.status = :status " +
            "ORDER BY r.createdAt DESC")
    List<InterviewRequest> findByStatus(@Param("status") RequestStatus status);
}