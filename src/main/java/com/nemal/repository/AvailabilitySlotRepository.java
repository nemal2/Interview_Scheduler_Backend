package com.nemal.repository;

import com.nemal.entity.AvailabilitySlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.springframework.data.repository.query.Param;


public interface AvailabilitySlotRepository extends JpaRepository<AvailabilitySlot, Long> {

    @Query("SELECT s FROM AvailabilitySlot s WHERE s.interviewer.id = :interviewerId AND s.status = 'AVAILABLE' AND (s.specificDate = :date OR (s.isRecurring AND s.dayOfWeek = :dayOfWeek))")
    List<AvailabilitySlot> findAvailableSlots(@Param("interviewerId") Long interviewerId,
                                              @Param("date") LocalDate date,
                                              @Param("dayOfWeek") DayOfWeek dayOfWeek);

    // FIXED: Native query for overlapping slots
    @Query(value = """
        SELECT * FROM availability_slots s
        WHERE s.interviewer_id = :interviewerId
        AND (
            (s.specific_date = :date 
             AND s.start_time < :endTime 
             AND s.end_time > :startTime)
            OR
            (s.is_recurring = true 
             AND s.day_of_week = :dayOfWeekStr
             AND s.start_time < :endTime 
             AND s.end_time > :startTime)
        )
        """, nativeQuery = true)
    List<AvailabilitySlot> findOverlapping(
            @Param("interviewerId") Long interviewerId,
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("dayOfWeekStr") String dayOfWeekStr);
}
