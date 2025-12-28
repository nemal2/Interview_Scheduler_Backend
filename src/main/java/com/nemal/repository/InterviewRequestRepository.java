package com.nemal.repository;

import com.nemal.entity.InterviewRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InterviewRequestRepository extends JpaRepository<InterviewRequest, Long> {
}
