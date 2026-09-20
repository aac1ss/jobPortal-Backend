package com.jobportal.backend.dto.company.response;

import lombok.Data;

import java.util.List;

@Data
public class RecruiterDashboardResponse {
    private Long companyId;
    private String companyName;
    private Boolean isCompanyVerified;
    private Integer totalJobsPosted;
    private Integer activeJobs;
    private Integer totalApplications;
    private Integer applicationsThisMonth;
    private Integer newApplications;
    private Integer shortlistedApplications;
    private Integer interviewScheduled;
    private Integer hiredCount;
    private Integer rejectedCount;
    private List<JobWithApplicationsResponse> recentJobs;
}