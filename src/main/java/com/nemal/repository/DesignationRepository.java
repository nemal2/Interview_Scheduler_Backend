package com.nemal.repository;

import com.nemal.entity.Designation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DesignationRepository extends JpaRepository<Designation, Long> {
    List<Designation> findByDepartmentIdAndIsActiveTrue(Long departmentId);
    List<Designation> findByIsActiveTrue();
    List<Designation> findByDepartmentId(Long departmentId);
}