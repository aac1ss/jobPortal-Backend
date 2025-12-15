package com.jobportal.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "company_profiles")
@Data
@EqualsAndHashCode(exclude = {"recruiter"})
public class CompanyProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_name", nullable = false, length = 200)
    private String companyName;

    @Column(name = "display_name", length = 200)
    private String displayName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "industry_id", nullable = false)
    private Industry industry;

    @Column(name = "company_size", nullable = false, length = 50)
    private String companySize;

    @Column(name = "website", length = 255)
    private String website;

    @Column(name = "description", columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(name = "contact_name", nullable = false, length = 100)
    private String contactName;

    @Column(name = "contact_email", nullable = false, length = 100)
    private String contactEmail;

    @Column(name = "contact_phone", length = 20)
    private String contactPhone;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "city", nullable = false, length = 100)
    private String city;

    @Column(name = "state", length = 100)
    private String state;

    @Column(name = "country", nullable = false, length = 100)
    private String country;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "cover_image_url", length = 500)
    private String coverImageUrl;

    @Column(name = "is_verified", nullable = false)
    private boolean isVerified = false;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "profile_complete", nullable = false)
    private boolean profileComplete = false;

    @Column(name = "linkedin_url", length = 255)
    private String linkedinUrl;

    @Column(name = "twitter_url", length = 255)
    private String twitterUrl;

    @Column(name = "facebook_url", length = 255)
    private String facebookUrl;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recruiter_id", unique = true, nullable = false)
    private User recruiter;

    // Helper Methods
    @PrePersist
    @PreUpdate
    public void checkProfileCompletion() {
        this.profileComplete = isProfileComplete();
    }

    public boolean isProfileComplete() {
        return companyName != null && !companyName.trim().isEmpty() &&
                industry != null &&
                companySize != null && !companySize.trim().isEmpty() &&
                description != null && description.length() >= 100 &&
                contactName != null && !contactName.trim().isEmpty() &&
                contactEmail != null && isValidEmail(contactEmail) &&
                city != null && !city.trim().isEmpty() &&
                country != null && !country.trim().isEmpty();
    }

    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }

    public int getCompletionPercentage() {
        int totalFields = 9;
        int completedFields = 0;

        if (companyName != null && !companyName.trim().isEmpty()) completedFields++;
        if (industry != null) completedFields++;
        if (companySize != null && !companySize.trim().isEmpty()) completedFields++;
        if (description != null && description.length() >= 100) completedFields++;
        if (contactName != null && !contactName.trim().isEmpty()) completedFields++;
        if (contactEmail != null && isValidEmail(contactEmail)) completedFields++;
        if (city != null && !city.trim().isEmpty()) completedFields++;
        if (country != null && !country.trim().isEmpty()) completedFields++;
        if (website != null && !website.trim().isEmpty()) completedFields++;

        return (completedFields * 100) / totalFields;
    }

    public void verify() {
        this.isVerified = true;
        this.verifiedAt = LocalDateTime.now();
    }

    public void unverify() {
        this.isVerified = false;
        this.verifiedAt = null;
    }
}