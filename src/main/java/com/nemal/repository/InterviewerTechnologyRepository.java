package com.nemal.repository;

import com.nemal.entity.InterviewerTechnology;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InterviewerTechnologyRepository extends JpaRepository<InterviewerTechnology, Long> {

    List<InterviewerTechnology> findByInterviewerId(Long interviewerId);

    List<InterviewerTechnology> findByTechnologyId(Long technologyId);

    List<InterviewerTechnology> findByInterviewerIdAndIsActiveTrue(Long interviewerId);

    boolean existsByInterviewerIdAndTechnologyId(Long interviewerId, Long technologyId);
}