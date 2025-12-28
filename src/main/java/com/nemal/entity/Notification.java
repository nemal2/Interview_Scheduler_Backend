package com.nemal.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private User recipient;

    private String subject;

    private String message;

    private String type;

    private boolean sent = false;

    private LocalDateTime sentAt;

    private LocalDateTime createdAt = LocalDateTime.now();

    private boolean isRead = false;

    public void setIsRead(boolean b) {

    }
}
