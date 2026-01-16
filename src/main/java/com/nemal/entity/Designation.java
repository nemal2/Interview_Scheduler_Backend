package com.nemal.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "designations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Designation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "hierarchy_level", nullable = false)
    private Integer hierarchyLevel;

    @ManyToOne
    @JoinColumn(name = "department_id")
    private Department department;

    @Column(name = "is_active")
    private boolean isActive = true;

    private String description;
}