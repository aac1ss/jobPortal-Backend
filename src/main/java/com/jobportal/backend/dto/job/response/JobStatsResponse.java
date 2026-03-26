package com.jobportal.backend.dto.job.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class JobStatsResponse {
    private long totalJobs;
    private long activeJobs;
    private long inactiveJobs;
    private long featuredJobs;
    private long totalApplications;

    @Builder.Default
    private double completionRate = 0.0;

    public double getCompletionRate() {
        if (totalJobs > 0) {
            return (activeJobs * 100.0) / totalJobs;
        }
        return 0.0;
    }
}