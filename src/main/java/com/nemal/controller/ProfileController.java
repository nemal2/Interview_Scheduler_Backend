package com.nemal.controller;

import com.nemal.dto.*;
import com.nemal.entity.User;
import com.nemal.service.ProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/profile")
    public ResponseEntity<ProfileDto> getProfile(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(profileService.getProfile(user));
    }

    @PutMapping("/profile")
    public ResponseEntity<ProfileDto> updateProfile(
            @AuthenticationPrincipal User user,
            @RequestBody ProfileUpdateDto updateDto
    ) {
        return ResponseEntity.ok(profileService.updateProfile(user, updateDto));
    }

    @GetMapping("/technologies")
    public ResponseEntity<List<TechnologyDto>> getAllTechnologies() {
        return ResponseEntity.ok(profileService.getAllTechnologies());
    }

    @PostMapping("/technologies")
    public ResponseEntity<TechnologyDto> createTechnology(@RequestBody CreateTechnologyDto dto) {
        return ResponseEntity.ok(profileService.createTechnology(dto));
    }

    @GetMapping("/profile/interviewer-technologies")
    public ResponseEntity<List<InterviewerTechnologyDto>> getInterviewerTechnologies(
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(profileService.getInterviewerTechnologies(user.getId()));
    }

    @PostMapping("/profile/interviewer-technologies")
    public ResponseEntity<InterviewerTechnologyDto> addInterviewerTechnology(
            @AuthenticationPrincipal User user,
            @RequestBody AddInterviewerTechnologyDto dto
    ) {
        return ResponseEntity.ok(profileService.addInterviewerTechnology(user, dto));
    }

    @DeleteMapping("/profile/interviewer-technologies/{id}")
    public ResponseEntity<Void> removeInterviewerTechnology(
            @AuthenticationPrincipal User user,
            @PathVariable Long id
    ) {
        profileService.removeInterviewerTechnology(user.getId(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/departments")
    public ResponseEntity<List<DepartmentDto>> getAllDepartments() {
        return ResponseEntity.ok(profileService.getAllDepartments());
    }

    @GetMapping("/designations")
    public ResponseEntity<List<DesignationDto>> getAllDesignations() {
        return ResponseEntity.ok(profileService.getAllDesignations());
    }
}