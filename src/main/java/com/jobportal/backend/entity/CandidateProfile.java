package com.jobportal.backend.entity;

import com.jobportal.backend.enums.Gender;
import com.jobportal.backend.enums.NoticePeriod;
import com.jobportal.backend.enums.ProfileVisibility;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Entity
@Table(name = "candidate_profiles")
@Data
@EqualsAndHashCode(of = "id")
public class CandidateProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Personal Information
    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 20)
    private Gender gender;

    @Column(name = "profile_picture_url", length = 500)
    private String profilePictureUrl;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "country", length = 100)
    private String country;

    // Professional Information
    @Column(name = "headline", length = 200)
    private String headline;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "total_experience_years")
    private Integer totalExperienceYears;

    @Column(name = "current_salary", precision = 12, scale = 2)
    private BigDecimal currentSalary;

    @Column(name = "expected_salary", precision = 12, scale = 2)
    private BigDecimal expectedSalary;

    @Column(name = "salary_currency", length = 3)
    private String salaryCurrency = "NPR";

    @Enumerated(EnumType.STRING)
    @Column(name = "notice_period", length = 30)
    private NoticePeriod noticePeriod = NoticePeriod.ONE_MONTH;

    // Collections stored as TEXT (not JSON)
    @Column(name = "education_json", columnDefinition = "TEXT")
    private String educationJson = "[]";

    @Column(name = "experience_json", columnDefinition = "TEXT")
    private String experienceJson = "[]";

    @Column(name = "skills", length = 1000)
    private String skills = "";

    @Column(name = "certifications_json", columnDefinition = "TEXT")
    private String certificationsJson = "[]";

    @Column(name = "languages_json", columnDefinition = "TEXT")
    private String languagesJson = "[]";

    // Job Preferences
    @Column(name = "preferred_job_types_json", columnDefinition = "TEXT")
    private String preferredJobTypesJson = "[]";

    @Column(name = "preferred_locations_json", columnDefinition = "TEXT")
    private String preferredLocationsJson = "[]";

    @Column(name = "preferred_industries_json", columnDefinition = "TEXT")
    private String preferredIndustriesJson = "[]";

    @Column(name = "remote_preference")
    private Boolean remotePreference;

    // Resume (Optional)
    @Column(name = "resume_url", length = 500)
    private String resumeUrl;

    @Column(name = "resume_file_name", length = 255)
    private String resumeFileName;

    @Column(name = "resume_original_name", length = 255)
    private String resumeOriginalName;

    @Column(name = "is_resume_uploaded", nullable = false)
    private boolean isResumeUploaded = false;

    // Status & Settings
    @Enumerated(EnumType.STRING)
    @Column(name = "profile_visibility", nullable = false, length = 20)
    private ProfileVisibility profileVisibility = ProfileVisibility.PRIVATE;

    @Column(name = "is_profile_complete", nullable = false)
    private boolean isProfileComplete = false;

    @Column(name = "completion_percentage", nullable = false)
    private Integer completionPercentage = 0;

    @Column(name = "is_featured", nullable = false)
    private boolean isFeatured = false;

    @Column(name = "is_actively_looking", nullable = false)
    private boolean isActivelyLooking = true;

    @Column(name = "available_from")
    private LocalDate availableFrom;

    @Column(name = "last_active_at")
    private LocalDateTime lastActiveAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Relationships
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @OneToMany(mappedBy = "candidate", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<JobApplication> jobApplications = new ArrayList<>();

    // Helper Methods
    @PrePersist
    @PreUpdate
    public void calculateCompletion() {
        int score = 0;
        // CV is optional, so max score is now 12 instead of 13
        int maxScore = 12;

        if (isValid(firstName)) score++;
        if (isValid(lastName)) score++;
        if (isValid(email)) score++;
        if (isValid(headline)) score++;
        if (isValid(summary) && summary.length() >= 50) score++;
        if (totalExperienceYears != null) score++;
        if (experienceJson != null && !experienceJson.equals("[]") && !experienceJson.equals("")) score++;
        if (educationJson != null && !educationJson.equals("[]") && !educationJson.equals("")) score++;
        if (skills != null && !skills.trim().isEmpty() && skills.split(",").length >= 3) score++;
        if (preferredJobTypesJson != null && !preferredJobTypesJson.equals("[]") && !preferredJobTypesJson.equals("")) score++;
        if (preferredLocationsJson != null && !preferredLocationsJson.equals("[]") && !preferredLocationsJson.equals("")) score++;
        // CV is optional - removed from completion calculation
        if (profilePictureUrl != null && !profilePictureUrl.trim().isEmpty()) score++;

        this.completionPercentage = (score * 100) / maxScore;
        this.isProfileComplete = this.completionPercentage >= 80;
        this.lastActiveAt = LocalDateTime.now();
    }

    private boolean isValid(String value) {
        return value != null && !value.trim().isEmpty();
    }

    public boolean canApplyForJobs() {
        // CV is optional - removed isResumeUploaded check
        // Actively looking is optional - removed isActivelyLooking check
        return isProfileComplete &&
                user != null &&
                user.isActive() &&
                user.isEmailVerified();
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public List<String> getSkillsList() {
        if (skills == null || skills.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.asList(skills.split("\\s*,\\s*"));
    }

    public void setSkillsList(List<String> skillList) {
        if (skillList == null || skillList.isEmpty()) {
            this.skills = "";
        } else {
            this.skills = String.join(",", skillList);
        }
    }

    public List<String> getPreferredJobTypesList() {
        try {
            if (preferredJobTypesJson != null && !preferredJobTypesJson.isEmpty()) {
                return Arrays.asList(preferredJobTypesJson.replace("[", "").replace("]", "").replace("\"", "").split(","));
            }
        } catch (Exception e) {
            return new ArrayList<>();
        }
        return new ArrayList<>();
    }

    public void setPreferredJobTypesList(List<String> jobTypes) {
        if (jobTypes == null || jobTypes.isEmpty()) {
            this.preferredJobTypesJson = "[]";
        } else {
            this.preferredJobTypesJson = "[\"" + String.join("\",\"", jobTypes) + "\"]";
        }
    }

    public List<String> getPreferredLocationsList() {
        try {
            if (preferredLocationsJson != null && !preferredLocationsJson.isEmpty()) {
                return Arrays.asList(preferredLocationsJson.replace("[", "").replace("]", "").replace("\"", "").split(","));
            }
        } catch (Exception e) {
            return new ArrayList<>();
        }
        return new ArrayList<>();
    }

    public void setPreferredLocationsList(List<String> locations) {
        if (locations == null || locations.isEmpty()) {
            this.preferredLocationsJson = "[]";
        } else {
            this.preferredLocationsJson = "[\"" + String.join("\",\"", locations) + "\"]";
        }
    }

    public void addSkill(String skill) {
        List<String> currentSkills = getSkillsList();
        if (!currentSkills.contains(skill)) {
            currentSkills.add(skill);
            setSkillsList(currentSkills);
        }
    }
}