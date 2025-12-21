package com.jobportal.backend.entity;

import com.jobportal.backend.enums.ExperienceLevel;
import com.jobportal.backend.enums.JobType;
import com.jobportal.backend.enums.SalaryType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Entity
@Table(name = "jobs")
@Data
@EqualsAndHashCode(exclude = {"company", "jobDescription"})
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_type", nullable = false, length = 50)
    private JobType jobType;

    @Enumerated(EnumType.STRING)
    @Column(name = "experience_level", nullable = false, length = 50)
    private ExperienceLevel experienceLevel;

    @Column(name = "location", nullable = false, length = 200)
    private String location;

    @Column(name = "salary_min", precision = 12, scale = 2)
    private BigDecimal salaryMin;

    @Column(name = "salary_max", precision = 12, scale = 2)
    private BigDecimal salaryMax;

    @Column(name = "salary_currency", length = 3)
    private String salaryCurrency = "NPR";

    @Enumerated(EnumType.STRING)
    @Column(name = "salary_type", length = 20)
    private SalaryType salaryType;

    @Column(name = "is_remote", nullable = false)
    private boolean isRemote = false;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "is_featured", nullable = false)
    private boolean isFeatured = false;

    @Column(name = "application_deadline")
    private LocalDateTime applicationDeadline;

    @Column(name = "total_applications", nullable = false)
    private int totalApplications = 0;

    @Column(name = "required_skills", length = 1000)
    private String requiredSkills = "";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private CompanyProfile company;

    @OneToOne(mappedBy = "job", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private JobDescription jobDescription;

    @OneToMany(mappedBy = "job", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<JobApplication> jobApplications = new ArrayList<>();

    // Helper Methods
    public List<String> getRequiredSkillsList() {
        if (requiredSkills == null || requiredSkills.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.asList(requiredSkills.split("\\s*,\\s*"));
    }

    public boolean isApplicationOpen() {
        if (!isActive) return false;
        if (applicationDeadline == null) return true;
        return applicationDeadline.isAfter(LocalDateTime.now());
    }

    public boolean canApply(CandidateProfile candidate) {
        return isApplicationOpen() &&
                candidate != null &&
                candidate.canApplyForJobs() &&
                !hasApplied(candidate);
    }

    public boolean hasApplied(CandidateProfile candidate) {
        if (candidate == null || candidate.getJobApplications() == null) return false;
        return candidate.getJobApplications().stream()
                .anyMatch(app -> app.getJob().getId().equals(this.id) && !app.isWithdrawn());
    }
}