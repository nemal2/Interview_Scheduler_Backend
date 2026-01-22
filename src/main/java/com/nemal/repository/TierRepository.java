package com.nemal.repository;

import com.nemal.entity.Tier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TierRepository extends JpaRepository<Tier, Long> {

    // Find active tiers
    List<Tier> findByIsActiveTrueOrderByTierOrderAsc();

    // Find active tiers by department
    List<Tier> findByDepartmentIdAndIsActiveTrueOrderByTierOrderAsc(Long departmentId);

    // Check for duplicate tier order in active tiers (for create)
    boolean existsByDepartmentIdAndTierOrderAndIsActiveTrue(Long departmentId, Integer tierOrder);

    // Check for duplicate tier order in active tiers excluding current tier (for update)
    boolean existsByDepartmentIdAndTierOrderAndIsActiveTrueAndIdNot(
            Long departmentId,
            Integer tierOrder,
            Long id
    );
}