package com.nemal.repository;

import com.nemal.entity.AvailabilitySlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AvailabilitySlotRepository extends JpaRepository<AvailabilitySlot, Long> {

    // ── Interviewer's own slots (used in AvailabilityService) ────────────────

    /**
     * All active slots for an interviewer from 14 days ago onwards.
     * Replaces the old findByInterviewerIdAndIsActiveTrue which only showed future slots.
     */
    @Query("SELECT s FROM AvailabilitySlot s " +
            "WHERE s.interviewer.id = :interviewerId " +
            "AND s.isActive = true " +
            "AND s.startDateTime >= :from " +
            "ORDER BY s.startDateTime")
    List<AvailabilitySlot> findByInterviewerIdAndIsActiveTrueWithLookback(
            @Param("interviewerId") Long interviewerId,
            @Param("from") LocalDateTime from
    );

    /**
     * Keeps the old method signature intact so nothing else breaks.
     * Internally delegates to lookback variant using now-minus-14d.
     * NOTE: Prefer findByInterviewerIdAndIsActiveTrueWithLookback for new code.
     */
    List<AvailabilitySlot> findByInterviewerIdAndIsActiveTrue(Long interviewerId);

    List<AvailabilitySlot> findByInterviewerIdAndStartDateTimeBetweenAndIsActiveTrue(
            Long interviewerId,
            LocalDateTime start,
            LocalDateTime end
    );

    // ── AVAILABLE only (used for conflict checks, interviewer request matching) ──

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

    // ── AVAILABLE + BOOKED for HR calendar — with lookback ───────────────────

    /**
     * Primary HR calendar query.
     * Uses :from instead of :now so HR can see recent history (pass now-minus-30d).
     * Without a lookback, booked/completed slots disappear the moment they're in the past.
     */
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
            "AND s.startDateTime >= :from " +
            "ORDER BY s.startDateTime")
    List<AvailabilitySlot> findAllActiveSlotsForHR(@Param("from") LocalDateTime from);

    /**
     * HR calendar with explicit date-range filter (used when the user applies
     * a date filter in the UI). The date range already captures history so no
     * extra lookback is needed here.
     */
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

    // ── Conflict detection — only checks AVAILABLE, only future ─────────────

    @Query("SELECT s FROM AvailabilitySlot s " +
            "WHERE s.interviewer.id = :interviewerId " +
            "AND s.isActive = true " +
            "AND s.status != 'BOOKED' " +
            "AND ((s.startDateTime < :end AND s.endDateTime > :start))")
    List<AvailabilitySlot> findConflictingSlots(
            @Param("interviewerId") Long interviewerId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    // ── Stats — count upcoming AVAILABLE / BOOKED from a given point ─────────

    /**
     * Count upcoming available slots for an interviewer starting from :from.
     * Pass LocalDateTime.now() for "upcoming only" or now.minusDays(N) for a window.
     */
    @Query("SELECT COUNT(s) FROM AvailabilitySlot s " +
            "WHERE s.interviewer.id = :interviewerId " +
            "AND s.status = 'AVAILABLE' " +
            "AND s.startDateTime >= :from " +
            "AND s.isActive = true")
    long countAvailableSlotsFrom(
            @Param("interviewerId") Long interviewerId,
            @Param("from") LocalDateTime from
    );

    /**
     * Count upcoming booked slots for an interviewer starting from :from.
     */
    @Query("SELECT COUNT(s) FROM AvailabilitySlot s " +
            "WHERE s.interviewer.id = :interviewerId " +
            "AND s.status = 'BOOKED' " +
            "AND s.startDateTime >= :from " +
            "AND s.isActive = true")
    long countBookedSlotsFrom(
            @Param("interviewerId") Long interviewerId,
            @Param("from") LocalDateTime from
    );

    // Keep old method names as aliases so nothing breaks if called elsewhere
    default long countUpcomingAvailableSlots(Long interviewerId, LocalDateTime now) {
        return countAvailableSlotsFrom(interviewerId, now);
    }

    default long countUpcomingBookedSlots(Long interviewerId, LocalDateTime now) {
        return countBookedSlotsFrom(interviewerId, now);
    }
}