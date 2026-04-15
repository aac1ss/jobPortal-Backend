package com.jobportal.backend.dto.admin.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardResponse {
    // Company Statistics
    private Long totalCompanies;
    private Long verifiedCompanies;
    private Long pendingVerification;
    private Long rejectedCompanies;

    // Job Statistics
    private Long activeJobs;
    private Long totalJobs;

    // Candidate Statistics
    private Long totalCandidates;
    private Long activeCandidates;

    // Registration Statistics
    private Long newRegistrationsToday;
    private Long newCandidatesToday;
    private Long newRecruitersToday;

    // Application Statistics
    private Long applicationsThisWeek;
    private Long applicationsToday;
    private Long totalApplications;

    // Additional Metrics
    private Double companyVerificationRate;
    private Double jobFillRate;
    private LocalDateTime lastUpdated;
}