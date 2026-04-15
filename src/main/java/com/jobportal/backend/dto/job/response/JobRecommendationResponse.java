package com.jobportal.backend.dto.job.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JobRecommendationResponse {

    // Job information
    private Long id;
    private String title;
    private Long companyId;
    private String companyName;
    private String companyLogo;
    private String industryName;

    // Job details
    private String location;
    private String jobType;
    private Boolean isRemote;
    private String experienceLevel;
    private LocalDateTime createdAt;
    private LocalDateTime applicationDeadline;
    private Boolean isApplicationOpen;

    // Salary information
    private BigDecimal salaryMin;
    private BigDecimal salaryMax;
    private String salaryCurrency;
    private String salaryType;

    // Recommendation scores
    private Double recommendationScore;
    private Integer matchScore;
    private Integer trendingRank;
    private Map<String, Double> scoreBreakdown;

    // Why this job was recommended
    private List<String> recommendationReasons;

    // Application status
    private Boolean hasApplied;
    private Boolean canApply;

    // Metadata
    private Integer totalApplications;
    private LocalDateTime lastActiveAt;

}