package com.jobportal.backend.service;

import com.jobportal.backend.dto.company.request.UpdateApplicationStatusRequest;
import com.jobportal.backend.dto.company.response.JobApplicationDetailResponse;
import com.jobportal.backend.dto.company.response.JobWithApplicationsResponse;
import com.jobportal.backend.dto.company.response.RecruiterDashboardResponse;
import com.jobportal.backend.dto.recruiter.response.CandidateProfileViewResponse;
import com.jobportal.backend.enums.ApplicationStatus;

import java.util.List;

public interface RecruiterService {
    RecruiterDashboardResponse getDashboard(Long recruiterId);

    List<JobWithApplicationsResponse> getAllJobsWithApplications(Long recruiterId);

    JobWithApplicationsResponse getJobApplications(Long recruiterId, Long jobId);

    JobApplicationDetailResponse getApplicationDetail(Long recruiterId, Long applicationId);

    JobApplicationDetailResponse updateApplicationStatus(
            Long recruiterId,
            Long applicationId,
            UpdateApplicationStatusRequest request);

    List<JobApplicationDetailResponse> getApplicationsByStatus(
            Long recruiterId,
            ApplicationStatus status);

    List<JobApplicationDetailResponse> searchApplications(
            Long recruiterId,
            Long jobId,
            ApplicationStatus status,
            String keyword);

    CandidateProfileViewResponse viewCandidateProfile(Long recruiterId, Long candidateId);
}