package com.jobportal.backend.entity;

import com.jobportal.backend.enums.RoleEnum;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Where(clause = "deleted_at IS NULL AND is_active = true")
@Data
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false, length = 100) // Changed from username
    private String fullName;

    @Column(unique = true, nullable = false, length = 100)
    private String email;

    @Column(nullable = false, length = 100)
    private String password;

    @ElementCollection(targetClass = RoleEnum.class, fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private Set<RoleEnum> roleEnums = new HashSet<>();

    @Column(name = "is_active")
    private boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "last_login_time")
    private LocalDateTime lastLoginTime;

    @Column(name = "login_count")
    private Integer loginCount = 0;

    @Column(name = "password_updated_at")
    private LocalDateTime passwordUpdatedAt;

    @Column(name = "failed_login_attempts")
    private Integer failedLoginAttempts = 0;

    @Column(name = "account_locked_until")
    private LocalDateTime accountLockedUntil;

    @Column(name = "email_verified")
    private boolean emailVerified = false;

    @Column(name = "email_verified_at")
    private LocalDateTime emailVerifiedAt;

    @Version
    private Long version;

    // Add this relationship
    @OneToOne(mappedBy = "recruiter", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private CompanyProfile companyProfile;

    public boolean hasCompanyProfile() {
        return this.companyProfile != null;
    }

    public boolean hasCompleteCompanyProfile() {
        return this.companyProfile != null && this.companyProfile.isProfileComplete();
    }

    public void verifyEmail() {
        this.emailVerified = true;
        this.emailVerifiedAt = LocalDateTime.now();
        this.isActive = true;
    }

    public void recordLogin() {
        this.lastLoginTime = LocalDateTime.now();
        this.loginCount = (this.loginCount == null) ? 1 : this.loginCount + 1;
        this.failedLoginAttempts = 0;
        this.accountLockedUntil = null;
    }

    public void recordFailedLogin(int maxLoginAttempts, int lockDurationMinutes) {
        this.failedLoginAttempts = (this.failedLoginAttempts == null) ? 1 : this.failedLoginAttempts + 1;

        if (this.failedLoginAttempts >= maxLoginAttempts) {
            this.accountLockedUntil = LocalDateTime.now().plusMinutes(lockDurationMinutes);
        }
    }

    public boolean isAccountLocked() {
        return accountLockedUntil != null && accountLockedUntil.isAfter(LocalDateTime.now());
    }

    public void unlockAccount() {
        this.failedLoginAttempts = 0;
        this.accountLockedUntil = null;
    }

    public void addRole(RoleEnum role) {
        if (roleEnums == null) {
            roleEnums = new HashSet<>();
        }
        roleEnums.add(role);
    }

    public void removeRole(RoleEnum role) {
        if (roleEnums != null) {
            roleEnums.remove(role);
        }
    }

    public boolean hasRole(RoleEnum role) {
        return roleEnums != null && roleEnums.contains(role);
    }
}