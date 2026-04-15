package com.jobportal.backend.service;

import com.jobportal.backend.entity.CandidateProfile;
import com.jobportal.backend.entity.Job;
import com.jobportal.backend.enums.JobType;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class JobTypeMatchService {

    public int calculateJobTypeMatchScore(CandidateProfile candidate, Job job) {
        List<String> preferredJobTypes = candidate.getPreferredJobTypesList();
        JobType jobType = job.getJobType();

        if (preferredJobTypes == null || preferredJobTypes.isEmpty() || jobType == null) {
            return 0;
        }

        // Check if preferred job types contain this job type
        boolean match = preferredJobTypes.stream()
                .anyMatch(preferred -> isJobTypeMatch(preferred, jobType));

        return match ? 10 : 0; // Job type contributes up to 10 points
    }

    private boolean isJobTypeMatch(String preferredJobType, JobType jobType) {
        try {
            JobType preferred = JobType.valueOf(preferredJobType.toUpperCase());
            return preferred == jobType;
        } catch (IllegalArgumentException e) {
            // Try case-insensitive comparison
            return preferredJobType.equalsIgnoreCase(jobType.name());
        }
    }
}