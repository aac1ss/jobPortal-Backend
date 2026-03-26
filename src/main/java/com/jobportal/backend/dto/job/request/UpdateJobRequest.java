package com.jobportal.backend.dto.job.request;

import com.jobportal.backend.enums.ExperienceLevel;
import com.jobportal.backend.enums.JobType;
import com.jobportal.backend.enums.SalaryType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class UpdateJobRequest {

    @Size(min = 5, max = 200, message = "Job title must be between 5 and 200 characters")
    private String title;

    private JobType jobType;

    private ExperienceLevel experienceLevel;

    @Size(max = 200, message = "Location is too long")
    private String location;

    @Size(min = 100, max = 5000, message = "Overview must be between 100 and 5000 characters")
    private String overview;

    @Size(min = 100, max = 5000, message = "Responsibilities must be between 100 and 5000 characters")
    private String responsibilities;

    @Size(min = 100, max = 5000, message = "Requirements must be between 100 and 5000 characters")
    private String requirements;

    private String niceToHave;

    private String benefits;

    private String applicationInstructions;

    @DecimalMin(value = "0.0", message = "Minimum salary cannot be negative")
    private BigDecimal salaryMin;

    @DecimalMin(value = "0.0", message = "Maximum salary cannot be negative")
    private BigDecimal salaryMax;

    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a 3-letter currency code")
    private String salaryCurrency;

    private SalaryType salaryType;

    private Boolean isRemote;

    @Future(message = "Application deadline must be in the future")
    private LocalDateTime applicationDeadline;

    private Boolean isActive;
}