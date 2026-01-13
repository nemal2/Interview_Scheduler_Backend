package com.nemal.service;

import com.nemal.dto.*;
import com.nemal.entity.*;
import com.nemal.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProfileService {

    private final UserRepository userRepository;
    private final TechnologyRepository technologyRepository;
    private final InterviewerTechnologyRepository interviewerTechnologyRepository;
    private final DepartmentRepository departmentRepository;
    private final DesignationRepository designationRepository;

    public ProfileService(
            UserRepository userRepository,
            TechnologyRepository technologyRepository,
            InterviewerTechnologyRepository interviewerTechnologyRepository,
            DepartmentRepository departmentRepository,
            DesignationRepository designationRepository
    ) {
        this.userRepository = userRepository;
        this.technologyRepository = technologyRepository;
        this.interviewerTechnologyRepository = interviewerTechnologyRepository;
        this.departmentRepository = departmentRepository;
        this.designationRepository = designationRepository;
    }

    public ProfileDto getProfile(User user) {
        User fullUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ProfileDto.from(fullUser);
    }

    @Transactional
    public ProfileDto updateProfile(User user, ProfileUpdateDto updateDto) {
        User fullUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Update basic info
        if (updateDto.firstName() != null) {
            fullUser.setFirstName(updateDto.firstName());
        }
        if (updateDto.lastName() != null) {
            fullUser.setLastName(updateDto.lastName());
        }
        if (updateDto.phone() != null) {
            fullUser.setPhone(updateDto.phone());
        }
        if (updateDto.profilePictureUrl() != null) {
            fullUser.setProfilePictureUrl(updateDto.profilePictureUrl());
        }
        if (updateDto.bio() != null) {
            fullUser.setBio(updateDto.bio());
        }
        if (updateDto.yearsOfExperience() != null) {
            fullUser.setYearsOfExperience(updateDto.yearsOfExperience());
        }

        // Update department
        if (updateDto.departmentId() != null) {
            Department dept = departmentRepository.findById(updateDto.departmentId())
                    .orElseThrow(() -> new RuntimeException("Department not found"));
            fullUser.setDepartment(dept);
        }

        // Update designation
        if (updateDto.designationId() != null) {
            Designation des = designationRepository.findById(updateDto.designationId())
                    .orElseThrow(() -> new RuntimeException("Designation not found"));
            fullUser.setCurrentDesignation(des);
        }

        userRepository.save(fullUser);
        return ProfileDto.from(fullUser);
    }

    public List<TechnologyDto> getAllTechnologies() {
        return technologyRepository.findAll().stream()
                .filter(Technology::isActive)
                .map(TechnologyDto::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public TechnologyDto createTechnology(CreateTechnologyDto dto) {
        // Check if technology already exists
        Technology existingTech = technologyRepository.findByNameIgnoreCase(dto.name());
        if (existingTech != null) {
            return TechnologyDto.from(existingTech);
        }

        Technology tech = Technology.builder()
                .name(dto.name())
                .category(dto.category() != null ? dto.category() : "General")
                .isActive(true)
                .build();

        return TechnologyDto.from(technologyRepository.save(tech));
    }

    public List<InterviewerTechnologyDto> getInterviewerTechnologies(Long userId) {
        return interviewerTechnologyRepository.findByInterviewerId(userId).stream()
                .filter(InterviewerTechnology::isActive)
                .map(InterviewerTechnologyDto::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public InterviewerTechnologyDto addInterviewerTechnology(User user, AddInterviewerTechnologyDto dto) {
        Technology tech = technologyRepository.findById(dto.technologyId())
                .orElseThrow(() -> new RuntimeException("Technology not found"));

        // Check if already exists and is active
        List<InterviewerTechnology> existing = interviewerTechnologyRepository.findByInterviewerId(user.getId());
        for (InterviewerTechnology it : existing) {
            if (it.getTechnology().getId().equals(tech.getId()) && it.isActive()) {
                throw new RuntimeException("Technology already added");
            }
        }

        InterviewerTechnology it = InterviewerTechnology.builder()
                .interviewer(user)
                .technology(tech)
                .yearsOfExperience(dto.yearsOfExperience())
                .isActive(true)
                .build();

        return InterviewerTechnologyDto.from(interviewerTechnologyRepository.save(it));
    }

    @Transactional
    public void removeInterviewerTechnology(Long userId, Long interviewerTechnologyId) {
        InterviewerTechnology it = interviewerTechnologyRepository.findById(interviewerTechnologyId)
                .orElseThrow(() -> new RuntimeException("Technology mapping not found"));

        if (!it.getInterviewer().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized: You can only remove your own technologies");
        }

        interviewerTechnologyRepository.delete(it);
    }

    public List<DepartmentDto> getAllDepartments() {
        return departmentRepository.findAll().stream()
                .map(DepartmentDto::from)
                .collect(Collectors.toList());
    }

    public List<DesignationDto> getAllDesignations() {
        return designationRepository.findAll().stream()
                .filter(Designation::isActive)
                .map(DesignationDto::from)
                .collect(Collectors.toList());
    }
}