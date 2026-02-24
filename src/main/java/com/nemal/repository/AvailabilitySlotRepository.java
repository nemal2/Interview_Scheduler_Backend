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

    // ── AVAILABLE only (used for conflict checks etc.) ───────────────────────

    @Query("SELECT DISTINCT s FROM AvailabilitySlot s " +
            "LEFT JOIN FETCH s.interviewer i " +
            "LEFT JOIN FETCH i.department " +
            "LEFT JOIN FETCH i.currentDesignation d " +
            "LEFT JOIN FETCH d.tier " +
            "LEFT JOIN FETCH i.interviewerTechnologies it " +
            "LEFT JOIN FETCH it.technology " +
            "WHERE s.status = 'AVAILABLE' " +
            "AND s.isActive = true " +
            "AND s.startDateTime >= :now " +
            "ORDER BY s.startDateTime")
    List<AvailabilitySlot> findAllAvailableSlots(@Param("now") LocalDateTime now);

    @Query("SELECT DISTINCT s FROM AvailabilitySlot s " +
            "LEFT JOIN FETCH s.interviewer i " +
            "LEFT JOIN FETCH i.department " +
            "LEFT JOIN FETCH i.currentDesignation d " +
            "LEFT JOIN FETCH d.tier " +
            "LEFT JOIN FETCH i.interviewerTechnologies it " +
            "LEFT JOIN FETCH it.technology " +
            "WHERE s.status = 'AVAILABLE' " +
            "AND s.isActive = true " +
            "AND s.startDateTime >= :start " +
            "AND s.endDateTime <= :end " +
            "ORDER BY s.startDateTime")
    List<AvailabilitySlot> findAllAvailableSlotsByDateRange(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    // ── AVAILABLE + BOOKED (used by HR calendar to show full picture) ────────

    @Query("SELECT DISTINCT s FROM AvailabilitySlot s " +
            "LEFT JOIN FETCH s.interviewer i " +
            "LEFT JOIN FETCH i.department " +
            "LEFT JOIN FETCH i.currentDesignation d " +
            "LEFT JOIN FETCH d.tier " +
            "LEFT JOIN FETCH i.interviewerTechnologies it " +
            "LEFT JOIN FETCH it.technology " +
            "LEFT JOIN FETCH s.interviewSchedule sch " +
            "LEFT JOIN FETCH sch.request " +
            "WHERE s.isActive = true " +
            "AND (s.status = 'AVAILABLE' OR s.status = 'BOOKED') " +
            "AND s.startDateTime >= :now " +
            "ORDER BY s.startDateTime")
    List<AvailabilitySlot> findAllActiveSlotsForHR(@Param("now") LocalDateTime now);

    @Query("SELECT DISTINCT s FROM AvailabilitySlot s " +
            "LEFT JOIN FETCH s.interviewer i " +
            "LEFT JOIN FETCH i.department " +
            "LEFT JOIN FETCH i.currentDesignation d " +
            "LEFT JOIN FETCH d.tier " +
            "LEFT JOIN FETCH i.interviewerTechnologies it " +
            "LEFT JOIN FETCH it.technology " +
            "LEFT JOIN FETCH s.interviewSchedule sch " +
            "LEFT JOIN FETCH sch.request " +
            "WHERE s.isActive = true " +
            "AND (s.status = 'AVAILABLE' OR s.status = 'BOOKED') " +
            "AND s.startDateTime >= :start " +
            "AND s.endDateTime <= :end " +
            "ORDER BY s.startDateTime")
    List<AvailabilitySlot> findAllActiveSlotsForHRByDateRange(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    // ── Conflict detection (only AVAILABLE slots) ────────────────────────────

    @Query("SELECT s FROM AvailabilitySlot s " +
            "WHERE s.interviewer.id = :interviewerId " +
            "AND s.isActive = true " +
            "AND ((s.startDateTime BETWEEN :start AND :end) " +
            "OR (s.endDateTime BETWEEN :start AND :end) " +
            "OR (s.startDateTime <= :start AND s.endDateTime >= :end))")
    List<AvailabilitySlot> findConflictingSlots(
            @Param("interviewerId") Long interviewerId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    // ── Stats ─────────────────────────────────────────────────────────────────

    @Query("SELECT COUNT(s) FROM AvailabilitySlot s " +
            "WHERE s.interviewer.id = :interviewerId " +
            "AND s.status = 'AVAILABLE' " +
            "AND s.startDateTime >= :now " +
            "AND s.isActive = true")
    long countUpcomingAvailableSlots(
            @Param("interviewerId") Long interviewerId,
            @Param("now") LocalDateTime now
    );

    @Query("SELECT COUNT(s) FROM AvailabilitySlot s " +
            "WHERE s.interviewer.id = :interviewerId " +
            "AND s.status = 'BOOKED' " +
            "AND s.startDateTime >= :now " +
            "AND s.isActive = true")
    long countUpcomingBookedSlots(
            @Param("interviewerId") Long interviewerId,
            @Param("now") LocalDateTime now
    );
}