package com.jobportal.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "email_verification_tokens")
@Data
public class EmailVerificationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 6)
    private String otp;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String userData;

    @Column(nullable = false)
    private LocalDateTime expiryDate;

    @Column(nullable = false)
    private boolean used = false;

    @Column(nullable = false)
    private int attempts = 0;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime usedAt;

    @Column(nullable = false, length = 45)
    private String ipAddress;

    @Column
    private String userAgent;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiryDate);
    }
}