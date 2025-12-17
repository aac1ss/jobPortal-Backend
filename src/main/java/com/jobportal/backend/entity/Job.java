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

    @Column(name = "salary_type", length = 20)
    private SalaryType salaryType; // MONTHLY, YEARLY, HOURLY, PROJECT_BASED

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
}