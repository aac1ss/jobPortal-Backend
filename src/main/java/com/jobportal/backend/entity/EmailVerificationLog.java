package com.jobportal.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "email_verification_logs")
@Data
public class EmailVerificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDateTime requestedAt;

    private LocalDateTime completedAt;

    @Column(nullable = false, length = 20)
    private String status; // SENT, VERIFIED, EXPIRED, FAILED

    @Column(length = 45)
    private String ipAddress;

    @Column
    private String userAgent;

    @PrePersist
    protected void onCreate() {
        requestedAt = LocalDateTime.now();
    }
}