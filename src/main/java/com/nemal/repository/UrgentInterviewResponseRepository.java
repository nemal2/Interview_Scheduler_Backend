package com.nemal.repository;

import com.nemal.entity.UrgentInterviewResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;

public interface UrgentInterviewResponseRepository extends JpaRepository<UrgentInterviewResponse, Long> {

    @Query("SELECT COUNT(r) FROM UrgentInterviewResponse r WHERE r.broadcast.createdAt BETWEEN :start AND :end")
    long countByTimeRange(LocalDateTime start, LocalDateTime end);
}
