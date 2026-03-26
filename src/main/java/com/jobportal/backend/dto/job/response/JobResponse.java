package com.jobportal.backend.dto.job.response;


import com.jobportal.backend.enums.ExperienceLevel;
import com.jobportal.backend.enums.JobType;
import com.jobportal.backend.enums.SalaryType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class JobResponse {

    private Long id;
    private String title;
    private JobType jobType;
    private ExperienceLevel experienceLevel;
    private String location;
    private BigDecimal salaryMin;
    private BigDecimal salaryMax;
    private String salaryCurrency;
    private SalaryType salaryType;
    private boolean isRemote;
    private boolean isActive;
    private boolean isFeatured;
    private int totalApplications;
    private LocalDateTime applicationDeadline;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime publishedAt;

    // Job description details
    private String overview;
    private String responsibilities;
    private String requirements;
    private String niceToHave;
    private String benefits;
    private String applicationInstructions;

    // Company info
    private Long companyId;
    private String companyName;
    private String companyLogo;
    private String industryName;
    private String companyLocation; // e.g., "Kathmandu, Nepal"

    // Formatted salary for display
    public String getFormattedSalary() {
        if (salaryMin == null && salaryMax == null) {
            return "Negotiable";
        } else if (salaryMin != null && salaryMax != null) {
            return String.format("NPR %s - %s per %s",
                    formatSalary(salaryMin),
                    formatSalary(salaryMax),
                    salaryType != null ? salaryType.toString().toLowerCase() : "month");
        } else if (salaryMin != null) {
            return String.format("NPR %s+ per %s",
                    formatSalary(salaryMin),
                    salaryType != null ? salaryType.toString().toLowerCase() : "month");
        } else {
            return String.format("Up to NPR %s per %s",
                    formatSalary(salaryMax),
                    salaryType != null ? salaryType.toString().toLowerCase() : "month");
        }
    }

    private String formatSalary(BigDecimal salary) {
        if (salary == null) return "";

        // Format salary in Nepali style (e.g., 50,000)
        return String.format("%,.0f", salary);
    }
}