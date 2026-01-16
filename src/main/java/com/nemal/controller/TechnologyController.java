package com.nemal.controller;

import com.nemal.dto.CreateTechnologyDto;
import com.nemal.dto.TechnologyDto;
import com.nemal.dto.UpdateTechnologyDto;
import com.nemal.service.TechnologyService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/technologies")
@CrossOrigin(origins = "http://localhost:5173")
public class TechnologyController {

    private final TechnologyService technologyService;

    public TechnologyController(TechnologyService technologyService) {
        this.technologyService = technologyService;
    }

    @GetMapping
    public ResponseEntity<List<TechnologyDto>> getAllTechnologies() {
        return ResponseEntity.ok(technologyService.getAllTechnologies());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TechnologyDto> getTechnologyById(@PathVariable Long id) {
        return ResponseEntity.ok(technologyService.getTechnologyById(id));
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<TechnologyDto>> getTechnologiesByCategory(
            @PathVariable String category) {
        return ResponseEntity.ok(technologyService.getTechnologiesByCategory(category));
    }

    @GetMapping("/categories")
    public ResponseEntity<List<String>> getAllCategories() {
        return ResponseEntity.ok(technologyService.getAllCategories());
    }

    @PostMapping
    public ResponseEntity<TechnologyDto> createTechnology(
            @RequestBody CreateTechnologyDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(technologyService.createTechnology(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TechnologyDto> updateTechnology(
            @PathVariable Long id,
            @RequestBody UpdateTechnologyDto dto) {
        return ResponseEntity.ok(technologyService.updateTechnology(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTechnology(@PathVariable Long id) {
        technologyService.deleteTechnology(id);
        return ResponseEntity.noContent().build();
    }
}