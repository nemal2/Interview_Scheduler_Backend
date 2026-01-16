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

    public ProfileService(
            UserRepository userRepository,
            TechnologyRepository technologyRepository,
            InterviewerTechnologyRepository interviewerTechnologyRepository,
            DepartmentRepository departmentRepository
    ) {
        this.userRepository = userRepository;
        this.technologyRepository = technologyRepository;
        this.interviewerTechnologyRepository = interviewerTechnologyRepository;
        this.departmentRepository = departmentRepository;
    }

    public ProfileDto getProfile(User user) {
        User freshUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ProfileDto.from(freshUser);
    }

    @Transactional
    public ProfileDto updateProfile(User user, ProfileUpdateDto updateDto) {
        User existingUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (updateDto.firstName() != null) {
            existingUser.setFirstName(updateDto.firstName());
        }
        if (updateDto.lastName() != null) {
            existingUser.setLastName(updateDto.lastName());
        }
        if (updateDto.phone() != null) {
            existingUser.setPhone(updateDto.phone());
        }
        if (updateDto.profilePictureUrl() != null) {
            existingUser.setProfilePictureUrl(updateDto.profilePictureUrl());
        }

        existingUser = userRepository.save(existingUser);
        return ProfileDto.from(existingUser);
    }

    // Technology methods removed - now in TechnologyService
    // getAllTechnologies() and createTechnology() moved to TechnologyService

    public List<InterviewerTechnologyDto> getInterviewerTechnologies(Long userId) {
        return interviewerTechnologyRepository.findByInterviewerId(userId).stream()
                .map(InterviewerTechnologyDto::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public InterviewerTechnologyDto addInterviewerTechnology(User user, AddInterviewerTechnologyDto dto) {
        Technology technology = technologyRepository.findById(dto.technologyId())
                .orElseThrow(() -> new RuntimeException("Technology not found"));

        InterviewerTechnology it = InterviewerTechnology.builder()
                .interviewer(user)
                .technology(technology)
                .yearsOfExperience(dto.yearsOfExperience())
                .isActive(true)
                .build();

        it = interviewerTechnologyRepository.save(it);
        return InterviewerTechnologyDto.from(it);
    }

    @Transactional
    public void removeInterviewerTechnology(Long userId, Long interviewerTechId) {
        InterviewerTechnology it = interviewerTechnologyRepository.findById(interviewerTechId)
                .orElseThrow(() -> new RuntimeException("Technology assignment not found"));

        if (!it.getInterviewer().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }

        interviewerTechnologyRepository.delete(it);
    }

    public List<DepartmentDto> getAllDepartments() {
        return departmentRepository.findAll().stream()
                .map(DepartmentDto::from)
                .collect(Collectors.toList());
    }
}