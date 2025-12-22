package com.jobportal.backend.service;

import com.jobportal.backend.dto.candidate.request.CandidateProfileRequest;
import com.jobportal.backend.dto.candidate.request.JobApplicationRequest;
import com.jobportal.backend.dto.candidate.request.ResumeUploadRequest;
import com.jobportal.backend.dto.candidate.response.CandidateDashboardResponse;
import com.jobportal.backend.dto.candidate.response.CandidateProfileResponse;
import com.jobportal.backend.dto.candidate.response.JobApplicationResponse;
import com.jobportal.backend.enums.ApplicationStatus;
import com.jobportal.backend.enums.ProfileVisibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CandidateService {
    CandidateProfileResponse createOrUpdateProfile(Long userId, CandidateProfileRequest request);

    CandidateProfileResponse getProfile(Long userId);

    CandidateProfileResponse uploadResume(Long userId, ResumeUploadRequest request);

    CandidateProfileResponse updateVisibility(Long userId, ProfileVisibility visibility);

    CandidateProfileResponse updateActiveStatus(Long userId, boolean isActivelyLooking);

    void deleteProfile(Long userId);

    CandidateProfileResponse getProfileForApplication(Long userId);

    JobApplicationResponse applyForJob(Long userId, JobApplicationRequest request);

    JobApplicationResponse withdrawApplication(Long userId, Long applicationId);

    List<JobApplicationResponse> getApplications(Long userId);

    Page<JobApplicationResponse> getApplications(Long userId, Pageable pageable);

    List<JobApplicationResponse> getApplicationsByStatus(Long userId, ApplicationStatus status);

    JobApplicationResponse getApplication(Long userId, Long applicationId);

    CandidateDashboardResponse getDashboard(Long userId);

    JobApplicationResponse updateApplication(Long userId, Long applicationId, JobApplicationRequest request);

    JobApplicationResponse toggleFavorite(Long userId, Long applicationId);

    boolean hasAppliedForJob(Long userId, Long jobId);

    JobApplicationResponse reapplyForJob(Long userId, Long applicationId, JobApplicationRequest request);
}