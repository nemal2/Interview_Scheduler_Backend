package com.nemal.repository;

import com.nemal.entity.InterviewPanel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InterviewPanelRepository extends JpaRepository<InterviewPanel, Long> {

    @Query("SELECT DISTINCT p FROM InterviewPanel p " +
            "LEFT JOIN FETCH p.panelRequests r " +
            "LEFT JOIN FETCH r.assignedInterviewer " +
            "LEFT JOIN FETCH r.requiredTechnologies " +
            "WHERE p.candidate.id = :candidateId " +
            "ORDER BY p.startDateTime DESC")
    List<InterviewPanel> findByCandidateId(@Param("candidateId") Long candidateId);

    @Query("SELECT DISTINCT p FROM InterviewPanel p " +
            "LEFT JOIN FETCH p.panelRequests r " +
            "LEFT JOIN FETCH r.assignedInterviewer " +
            "LEFT JOIN FETCH r.requiredTechnologies " +
            "WHERE p.requestedBy.id = :userId " +
            "ORDER BY p.startDateTime DESC")
    List<InterviewPanel> findByRequestedById(@Param("userId") Long userId);

    @Query("SELECT DISTINCT p FROM InterviewPanel p " +
            "LEFT JOIN FETCH p.panelRequests r " +
            "LEFT JOIN FETCH r.assignedInterviewer " +
            "LEFT JOIN FETCH r.requiredTechnologies " +
            "WHERE p.id = :id")
    Optional<InterviewPanel> findByIdWithDetails(@Param("id") Long id);
}