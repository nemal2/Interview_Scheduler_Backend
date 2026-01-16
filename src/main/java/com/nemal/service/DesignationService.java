package com.nemal.service;

import com.nemal.dto.CreateDesignationDto;
import com.nemal.dto.DesignationDto;
import com.nemal.dto.UpdateDesignationDto;
import com.nemal.entity.Department;
import com.nemal.entity.Designation;
import com.nemal.repository.DepartmentRepository;
import com.nemal.repository.DesignationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DesignationService {

    private final DesignationRepository designationRepository;
    private final DepartmentRepository departmentRepository;

    public DesignationService(DesignationRepository designationRepository,
                              DepartmentRepository departmentRepository) {
        this.designationRepository = designationRepository;
        this.departmentRepository = departmentRepository;
    }

    public List<DesignationDto> getAllDesignations() {
        return designationRepository.findByIsActiveTrue().stream()
                .map(DesignationDto::from)
                .collect(Collectors.toList());
    }

    public List<DesignationDto> getDesignationsByDepartment(Long departmentId) {
        return designationRepository.findByDepartmentIdAndIsActiveTrue(departmentId).stream()
                .map(DesignationDto::from)
                .collect(Collectors.toList());
    }

    public DesignationDto getDesignationById(Long id) {
        Designation designation = designationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Designation not found"));
        return DesignationDto.from(designation);
    }

    @Transactional
    public DesignationDto createDesignation(CreateDesignationDto dto) {
        Department department = departmentRepository.findById(dto.departmentId())
                .orElseThrow(() -> new RuntimeException("Department not found"));

        Designation designation = Designation.builder()
                .name(dto.name())
                .hierarchyLevel(dto.hierarchyLevel())
                .department(department)
                .description(dto.description())
                .isActive(true)
                .build();

        designation = designationRepository.save(designation);
        return DesignationDto.from(designation);
    }

    @Transactional
    public DesignationDto updateDesignation(Long id, UpdateDesignationDto dto) {
        Designation designation = designationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Designation not found"));

        if (dto.name() != null) {
            designation.setName(dto.name());
        }
        if (dto.hierarchyLevel() != null) {
            designation.setHierarchyLevel(dto.hierarchyLevel());
        }
        if (dto.description() != null) {
            designation.setDescription(dto.description());
        }
        if (dto.isActive() != null) {
            designation.setActive(dto.isActive());
        }

        designation = designationRepository.save(designation);
        return DesignationDto.from(designation);
    }

    @Transactional
    public void deleteDesignation(Long id) {
        Designation designation = designationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Designation not found"));
        designation.setActive(false);
        designationRepository.save(designation);
    }
}