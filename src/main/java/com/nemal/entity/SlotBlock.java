package com.nemal.entity;

import com.nemal.enums.BlockType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "slot_blocks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SlotBlock {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private User interviewer;

    private LocalDateTime startDateTime;

    private LocalDateTime endDateTime;

    private String reason;

    @Enumerated(EnumType.STRING)
    private BlockType type;
}