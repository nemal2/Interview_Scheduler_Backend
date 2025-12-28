package com.nemal.repository;

import com.nemal.entity.InterviewerTechnology;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface InterviewerTechnologyRepository extends JpaRepository<InterviewerTechnology, Long> {

    @Query("SELECT it FROM InterviewerTechnology it WHERE it.interviewer.id = :interviewerId")
    List<InterviewerTechnology> findByInterviewerId(Long interviewerId);
}