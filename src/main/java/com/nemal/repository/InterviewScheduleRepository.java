package com.nemal.repository;

import com.nemal.entity.InterviewSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InterviewScheduleRepository extends JpaRepository<InterviewSchedule, Long> {

    /**
     * Find the schedule linked to a specific InterviewRequest.
     * Used during cancellation so we can cancel the schedule even after
     * the slot's interviewSchedule FK has been nulled out.
     */
    Optional<InterviewSchedule> findByRequestId(Long requestId);
}