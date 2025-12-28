package com.nemal.repository;

import com.nemal.entity.InterviewSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;

public interface InterviewScheduleRepository extends JpaRepository<InterviewSchedule, Long> {

    @Query("SELECT COUNT(s) FROM InterviewSchedule s WHERE s.startDateTime BETWEEN :start AND :end AND (:deptId IS NULL OR s.interviewer.department.id = :deptId)")
    long countByDateRange(LocalDateTime start, LocalDateTime end, Long deptId);
}