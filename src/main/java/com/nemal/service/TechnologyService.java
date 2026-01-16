package com.nemal.service;

import com.nemal.dto.CreateTechnologyDto;
import com.nemal.dto.TechnologyDto;
import com.nemal.dto.UpdateTechnologyDto;
import com.nemal.entity.Technology;
import com.nemal.repository.TechnologyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TechnologyService {

    private final TechnologyRepository technologyRepository;

    public TechnologyService(TechnologyRepository technologyRepository) {
        this.technologyRepository = technologyRepository;
    }

    public List<TechnologyDto> getAllTechnologies() {
        return technologyRepository.findAll().stream()
                .filter(Technology::isActive)
                .map(TechnologyDto::from)
                .collect(Collectors.toList());
    }

    public TechnologyDto getTechnologyById(Long id) {
        Technology technology = technologyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Technology not found"));
        return TechnologyDto.from(technology);
    }

    public List<TechnologyDto> getTechnologiesByCategory(String category) {
        return technologyRepository.findAll().stream()
                .filter(t -> t.isActive() && t.getCategory().equalsIgnoreCase(category))
                .map(TechnologyDto::from)
                .collect(Collectors.toList());
    }

    public List<String> getAllCategories() {
        return technologyRepository.findAll().stream()
                .filter(Technology::isActive)
                .map(Technology::getCategory)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    @Transactional
    public TechnologyDto createTechnology(CreateTechnologyDto dto) {
        // Check if technology already exists
        Technology existing = technologyRepository.findByNameIgnoreCase(dto.name());
        if (existing != null) {
            throw new RuntimeException("Technology with this name already exists");
        }

        Technology technology = Technology.builder()
                .name(dto.name())
                .category(dto.category())
                .isActive(true)
                .build();

        technology = technologyRepository.save(technology);
        return TechnologyDto.from(technology);
    }

    @Transactional
    public TechnologyDto updateTechnology(Long id, UpdateTechnologyDto dto) {
        Technology technology = technologyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Technology not found"));

        if (dto.name() != null) {
            // Check if name conflicts with another technology
            Technology existing = technologyRepository.findByNameIgnoreCase(dto.name());
            if (existing != null && !existing.getId().equals(id)) {
                throw new RuntimeException("Technology with this name already exists");
            }
            technology.setName(dto.name());
        }
        if (dto.category() != null) {
            technology.setCategory(dto.category());
        }
        if (dto.isActive() != null) {
            technology.setActive(dto.isActive());
        }

        technology = technologyRepository.save(technology);
        return TechnologyDto.from(technology);
    }

    @Transactional
    public void deleteTechnology(Long id) {
        Technology technology = technologyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Technology not found"));
        technology.setActive(false);
        technologyRepository.save(technology);
    }
}