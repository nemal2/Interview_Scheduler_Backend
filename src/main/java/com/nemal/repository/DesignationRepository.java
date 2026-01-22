package com.nemal.repository;

import com.nemal.entity.Designation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DesignationRepository extends JpaRepository<Designation, Long> {

    // For create: Check if name exists in active designations for a department
    boolean existsByDepartmentIdAndNameAndIsActiveTrue(Long departmentId, String name);

    // For create: Check if name exists in active designations for a department (case-insensitive)
    @Query("SELECT CASE WHEN COUNT(d) > 0 THEN true ELSE false END FROM Designation d " +
            "WHERE d.department.id = :departmentId AND LOWER(d.name) = LOWER(:name) AND d.isActive = true")
    boolean existsByDepartmentIdAndNameIgnoreCaseAndIsActiveTrue(Long departmentId, String name);

    // For create: Check active only for tier and level
    boolean existsByTierIdAndLevelOrderAndIsActiveTrue(Long tierId, Integer levelOrder);

    // For update: Check active only, excluding self
    boolean existsByTierIdAndLevelOrderAndIsActiveTrueAndIdNot(Long tierId, Integer levelOrder, Long id);

    // For update: Check if name exists excluding self
    boolean existsByDepartmentIdAndNameAndIsActiveTrueAndIdNot(Long departmentId, String name, Long id);

    // Existing methods (add if missing, based on your service usage)
    List<Designation> findByIsActiveTrue();
    List<Designation> findByDepartmentIdAndIsActiveTrue(Long departmentId);
    List<Designation> findByTierIdAndIsActiveTrueOrderByLevelOrderAsc(Long tierId);
}