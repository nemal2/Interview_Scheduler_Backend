package com.nemal.repository;

import com.nemal.entity.User;
import com.nemal.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    List<User> findByRole(Role role);

    @Query("SELECT u FROM User u WHERE u.department.id = :deptId AND u.role = :role")
    List<User> findByDepartmentAndRole(Long deptId, Role role);

    List<User> findByCurrentDesignationIdInAndRole(List<Long> eligibleDesignationIds, Role role);
}