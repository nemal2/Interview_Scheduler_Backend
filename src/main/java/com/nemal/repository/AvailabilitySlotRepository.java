package com.nemal.repository;

import com.nemal.entity.AvailabilitySlot;
import com.nemal.enums.SlotStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AvailabilitySlotRepository extends JpaRepository<AvailabilitySlot, Long> {

    List<AvailabilitySlot> findByInterviewerIdAndIsActiveTrue(Long interviewerId);

    List<AvailabilitySlot> findByInterviewerIdAndStartDateTimeBetweenAndIsActiveTrue(
            Long interviewerId,
            LocalDateTime start,
            LocalDateTime end
    );

    @Query("SELECT a FROM AvailabilitySlot a WHERE a.interviewer.id = :interviewerId " +
            "AND a.startDateTime >= :start AND a.endDateTime <= :end " +
            "AND a.status = :status AND a.isActive = true")
    List<AvailabilitySlot> findAvailableSlots(
            @Param("interviewerId") Long interviewerId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("status") SlotStatus status
    );

    @Query("SELECT a FROM AvailabilitySlot a WHERE a.interviewer.id = :interviewerId " +
            "AND ((a.startDateTime BETWEEN :start AND :end) OR (a.endDateTime BETWEEN :start AND :end) " +
            "OR (a.startDateTime <= :start AND a.endDateTime >= :end)) " +
            "AND a.isActive = true")
    List<AvailabilitySlot> findConflictingSlots(
            @Param("interviewerId") Long interviewerId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("SELECT COUNT(a) FROM AvailabilitySlot a WHERE a.interviewer.id = :interviewerId " +
            "AND a.status = 'AVAILABLE' AND a.startDateTime >= :now AND a.isActive = true")
    long countUpcomingAvailableSlots(@Param("interviewerId") Long interviewerId, @Param("now") LocalDateTime now);

    @Query("SELECT COUNT(a) FROM AvailabilitySlot a WHERE a.interviewer.id = :interviewerId " +
            "AND a.status = 'BOOKED' AND a.startDateTime >= :now AND a.isActive = true")
    long countUpcomingBookedSlots(@Param("interviewerId") Long interviewerId, @Param("now") LocalDateTime now);
}