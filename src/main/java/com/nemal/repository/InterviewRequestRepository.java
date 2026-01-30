// InterviewRequestRepository.java
package com.nemal.repository;

import com.nemal.entity.InterviewRequest;
import com.nemal.enums.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface InterviewRequestRepository extends JpaRepository<InterviewRequest, Long> {

    List<InterviewRequest> findByRequestedById(Long requestedById);

    List<InterviewRequest> findByAssignedInterviewerId(Long interviewerId);

    List<InterviewRequest> findByAssignedInterviewerIdAndStatus(Long interviewerId, RequestStatus status);

    @Query("SELECT ir FROM InterviewRequest ir WHERE ir.assignedInterviewer.id = :interviewerId " +
            "AND ir.status = 'PENDING' ORDER BY ir.isUrgent DESC, ir.createdAt ASC")
    List<InterviewRequest> findPendingRequestsForInterviewer(@Param("interviewerId") Long interviewerId);

    @Query("SELECT COUNT(ir) FROM InterviewRequest ir WHERE ir.assignedInterviewer.id = :interviewerId " +
            "AND ir.status = 'PENDING'")
    long countPendingRequestsForInterviewer(@Param("interviewerId") Long interviewerId);
}