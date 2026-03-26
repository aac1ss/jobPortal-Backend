package com.jobportal.backend.dto.company.response;

import lombok.Data;

import java.util.List;

@Data
public class JobWithApplicationsResponse {
    private Long jobId;
    private String jobTitle;
    private String jobType;
    private String location;
    private Boolean isRemote;
    private Boolean isActive;
    private Integer totalApplications;
    private Integer activeApplications;
    private Integer withdrawnApplications;
    private List<JobApplicationDetailResponse> applications;
}
