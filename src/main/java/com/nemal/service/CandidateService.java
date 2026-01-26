package com.nemal.service;

import com.nemal.dto.CandidateDto;
import com.nemal.dto.CreateCandidateDto;
import com.nemal.dto.UpdateCandidateDto;
import com.nemal.entity.Candidate;
import com.nemal.entity.Department;
import com.nemal.entity.Designation;
import com.nemal.enums.CandidateStatus;
import com.nemal.repository.CandidateRepository;
import com.nemal.repository.DepartmentRepository;
import com.nemal.repository.DesignationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CandidateService {

    private final CandidateRepository candidateRepository;
    private final DepartmentRepository departmentRepository;
    private final DesignationRepository designationRepository;

    public CandidateService(
            CandidateRepository candidateRepository,
            DepartmentRepository departmentRepository,
            DesignationRepository designationRepository
    ) {
        this.candidateRepository = candidateRepository;
        this.departmentRepository = departmentRepository;
        this.designationRepository = designationRepository;
    }

    public List<CandidateDto> getAllCandidates() {
        return candidateRepository.findByIsActiveTrueOrderByAppliedAtDesc().stream()
                .map(CandidateDto::from)
                .collect(Collectors.toList());
    }

    public CandidateDto getCandidateById(Long id) {
        Candidate candidate = candidateRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new RuntimeException("Candidate not found"));
        return CandidateDto.from(candidate);
    }

    public List<CandidateDto> getCandidatesByDepartment(Long departmentId) {
        return candidateRepository.findByDepartmentIdAndIsActiveTrueOrderByAppliedAtDesc(departmentId)
                .stream()
                .map(CandidateDto::from)
                .collect(Collectors.toList());
    }

    public List<CandidateDto> getCandidatesByStatus(CandidateStatus status) {
        return candidateRepository.findByStatusAndIsActiveTrueOrderByAppliedAtDesc(status)
                .stream()
                .map(CandidateDto::from)
                .collect(Collectors.toList());
    }

    public List<CandidateDto> searchCandidates(String searchTerm) {
        return candidateRepository.searchCandidates(searchTerm).stream()
                .map(CandidateDto::from)
                .collect(Collectors.toList());
    }

    public List<CandidateDto> findWithFilters(Long departmentId, CandidateStatus status, String searchTerm) {
        List<Candidate> candidates;

        // If no filters, get all
        if (departmentId == null && status == null && (searchTerm == null || searchTerm.trim().isEmpty())) {
            candidates = candidateRepository.findByIsActiveTrueOrderByAppliedAtDesc();
        }
        // If only department filter
        else if (departmentId != null && status == null && (searchTerm == null || searchTerm.trim().isEmpty())) {
            candidates = candidateRepository.findByDepartmentIdAndIsActiveTrueOrderByAppliedAtDesc(departmentId);
        }
        // If only status filter
        else if (departmentId == null && status != null && (searchTerm == null || searchTerm.trim().isEmpty())) {
            candidates = candidateRepository.findByStatusAndIsActiveTrueOrderByAppliedAtDesc(status);
        }
        // If department and status
        else if (departmentId != null && status != null && (searchTerm == null || searchTerm.trim().isEmpty())) {
            candidates = candidateRepository.findByDepartmentIdAndStatusAndIsActiveTrueOrderByAppliedAtDesc(departmentId, status);
        }
        // If search term exists, use search and then filter in memory
        else {
            String cleanSearchTerm = searchTerm != null ? searchTerm.trim() : "";
            if (!cleanSearchTerm.isEmpty()) {
                candidates = candidateRepository.searchCandidates(cleanSearchTerm);
            } else {
                candidates = candidateRepository.findByIsActiveTrueOrderByAppliedAtDesc();
            }

            // Apply additional filters in memory
            if (departmentId != null) {
                final Long deptId = departmentId;
                candidates = candidates.stream()
                        .filter(c -> c.getDepartment() != null && c.getDepartment().getId().equals(deptId))
                        .collect(Collectors.toList());
            }
            if (status != null) {
                final CandidateStatus stat = status;
                candidates = candidates.stream()
                        .filter(c -> c.getStatus() == stat)
                        .collect(Collectors.toList());
            }
        }

        return candidates.stream()
                .map(CandidateDto::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public CandidateDto createCandidate(CreateCandidateDto dto) {
        // Validate email uniqueness
        if (candidateRepository.existsByEmailAndIsActiveTrue(dto.email())) {
            throw new IllegalArgumentException("Candidate with email " + dto.email() + " already exists");
        }

        // Validate department
        Department department = null;
        if (dto.departmentId() != null) {
            department = departmentRepository.findById(dto.departmentId())
                    .orElseThrow(() -> new RuntimeException("Department not found"));
        }

        // Validate designation
        Designation designation = null;
        if (dto.targetDesignationId() != null) {
            designation = designationRepository.findById(dto.targetDesignationId())
                    .orElseThrow(() -> new RuntimeException("Designation not found"));

            // Verify designation belongs to the department
            if (department != null && designation.getDepartment() != null &&
                    !designation.getDepartment().getId().equals(department.getId())) {
                throw new IllegalArgumentException(
                        "Designation does not belong to the selected department");
            }

            // Auto-set department from designation if not provided
            if (department == null && designation.getDepartment() != null) {
                department = designation.getDepartment();
            }
        }

        Candidate candidate = Candidate.builder()
                .name(dto.name())
                .email(dto.email())
                .phone(dto.phone())
                .department(department)
                .targetDesignation(designation)
                .status(CandidateStatus.APPLIED)
                .resumeUrl(dto.resumeUrl())
                .notes(dto.notes())
                .yearsOfExperience(dto.yearsOfExperience())
                .isActive(true)
                .build();

        candidate = candidateRepository.save(candidate);
        return CandidateDto.from(candidate);
    }

    @Transactional
    public CandidateDto updateCandidate(Long id, UpdateCandidateDto dto) {
        Candidate candidate = candidateRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new RuntimeException("Candidate not found"));

        if (dto.name() != null) {
            candidate.setName(dto.name());
        }

        if (dto.email() != null && !dto.email().equals(candidate.getEmail())) {
            if (candidateRepository.existsByEmailAndIsActiveTrueAndIdNot(dto.email(), id)) {
                throw new IllegalArgumentException("Email already exists");
            }
            candidate.setEmail(dto.email());
        }

        if (dto.phone() != null) {
            candidate.setPhone(dto.phone());
        }

        if (dto.departmentId() != null) {
            Department department = departmentRepository.findById(dto.departmentId())
                    .orElseThrow(() -> new RuntimeException("Department not found"));
            candidate.setDepartment(department);
        }

        if (dto.targetDesignationId() != null) {
            Designation designation = designationRepository.findById(dto.targetDesignationId())
                    .orElseThrow(() -> new RuntimeException("Designation not found"));

            // Verify designation belongs to department
            if (candidate.getDepartment() != null && designation.getDepartment() != null &&
                    !designation.getDepartment().getId().equals(candidate.getDepartment().getId())) {
                throw new IllegalArgumentException(
                        "Designation does not belong to the candidate's department");
            }

            candidate.setTargetDesignation(designation);
        }

        if (dto.status() != null) {
            candidate.setStatus(dto.status());
        }

        if (dto.resumeUrl() != null) {
            candidate.setResumeUrl(dto.resumeUrl());
        }

        if (dto.notes() != null) {
            candidate.setNotes(dto.notes());
        }

        if (dto.yearsOfExperience() != null) {
            candidate.setYearsOfExperience(dto.yearsOfExperience());
        }

        if (dto.isActive() != null) {
            candidate.setActive(dto.isActive());
        }

        candidate = candidateRepository.save(candidate);
        return CandidateDto.from(candidate);
    }

    @Transactional
    public void deleteCandidate(Long id) {
        Candidate candidate = candidateRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new RuntimeException("Candidate not found"));

        // Soft delete
        candidate.setActive(false);
        candidateRepository.save(candidate);
    }
}