package com.nemal.repository;

import com.nemal.entity.Candidate;
import com.nemal.enums.CandidateStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CandidateRepository extends JpaRepository<Candidate, Long> {

    // Find all active candidates
    List<Candidate> findByIsActiveTrueOrderByAppliedAtDesc();

    // Find by department
    List<Candidate> findByDepartmentIdAndIsActiveTrueOrderByAppliedAtDesc(Long departmentId);

    // Find by status
    List<Candidate> findByStatusAndIsActiveTrueOrderByAppliedAtDesc(CandidateStatus status);

    // Find by department and status
    List<Candidate> findByDepartmentIdAndStatusAndIsActiveTrueOrderByAppliedAtDesc(
            Long departmentId, CandidateStatus status);

    // Check if email exists (for active candidates)
    boolean existsByEmailAndIsActiveTrue(String email);

    // Check if email exists excluding a specific candidate (for updates)
    boolean existsByEmailAndIsActiveTrueAndIdNot(String email, Long id);

    // Search candidates by name or email
    @Query("SELECT c FROM Candidate c WHERE c.isActive = true AND " +
            "(LOWER(c.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(c.email) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
            "ORDER BY c.appliedAt DESC")
    List<Candidate> searchCandidates(@Param("searchTerm") String searchTerm);

    Optional<Candidate> findByIdAndIsActiveTrue(Long id);
}