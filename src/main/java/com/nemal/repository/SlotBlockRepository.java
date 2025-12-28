package com.nemal.repository;

import com.nemal.entity.SlotBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface SlotBlockRepository extends JpaRepository<SlotBlock, Long> {

    @Query("SELECT b FROM SlotBlock b WHERE b.interviewer.id = :interviewerId AND b.startDateTime < :end AND b.endDateTime > :start")
    List<SlotBlock> findOverlappingBlocks(Long interviewerId, LocalDateTime start, LocalDateTime end);
}
