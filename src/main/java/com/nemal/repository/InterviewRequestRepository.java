// InterviewRequestRepository.java
package com.nemal.repository;

import com.nemal.entity.InterviewRequest;
import com.nemal.enums.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface InterviewRequestRepository extends JpaRepository<InterviewRequest, Long> {

    // Find all requests for a specific interviewer
    List<InterviewRequest> findByAssignedInterviewerId(Long interviewerId);

    // Find upcoming interviews for interviewer (ACCEPTED only, future dates)
    @Query("SELECT ir FROM InterviewRequest ir WHERE ir.assignedInterviewer.id = :interviewerId " +
            "AND ir.status = 'ACCEPTED' " +
            "AND ir.preferredStartDateTime >= :now " +
            "ORDER BY ir.preferredStartDateTime ASC")
    List<InterviewRequest> findUpcomingInterviewsForInterviewer(
            @Param("interviewerId") Long interviewerId,
            @Param("now") LocalDateTime now);

    // Count upcoming interviews for interviewer
    @Query("SELECT COUNT(ir) FROM InterviewRequest ir WHERE ir.assignedInterviewer.id = :interviewerId " +
            "AND ir.status = 'ACCEPTED' " +
            "AND ir.preferredStartDateTime >= CURRENT_TIMESTAMP")
    long countUpcomingInterviewsForInterviewer(@Param("interviewerId") Long interviewerId);

    // Find all requests created by an HR user
    List<InterviewRequest> findByRequestedById(Long requestedById);

    // Find requests by status
    List<InterviewRequest> findByStatus(RequestStatus status);

    // Find today's interviews for interviewer
    @Query(value = "SELECT ir.* FROM interview_requests ir " +
            "WHERE ir.assigned_interviewer_id = :interviewerId " +
            "AND ir.status = 'ACCEPTED' " +
            "AND DATE(ir.preferred_start_date_time) = CURRENT_DATE " +
            "ORDER BY ir.preferred_start_date_time ASC",
            nativeQuery = true)
    List<InterviewRequest> findTodaysInterviewsForInterviewer(@Param("interviewerId") Long interviewerId);

    // Find this week's interviews for interviewer
    @Query("SELECT ir FROM InterviewRequest ir WHERE ir.assignedInterviewer.id = :interviewerId " +
            "AND ir.status = 'ACCEPTED' " +
            "AND ir.preferredStartDateTime >= :weekStart " +
            "AND ir.preferredStartDateTime < :weekEnd " +
            "ORDER BY ir.preferredStartDateTime ASC")
    List<InterviewRequest> findThisWeeksInterviewsForInterviewer(
            @Param("interviewerId") Long interviewerId,
            @Param("weekStart") LocalDateTime weekStart,
            @Param("weekEnd") LocalDateTime weekEnd);
}