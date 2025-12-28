package com.nemal.repository;

import com.nemal.entity.DesignationInterviewRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface DesignationInterviewRuleRepository extends JpaRepository<DesignationInterviewRule, Long> {

    @Query("SELECT r FROM DesignationInterviewRule r WHERE r.candidateDesignation.id = :candidateDesignationId AND r.allowed = true")
    List<DesignationInterviewRule> findByCandidateDesignationIdAndAllowedTrue(Long candidateDesignationId);
}