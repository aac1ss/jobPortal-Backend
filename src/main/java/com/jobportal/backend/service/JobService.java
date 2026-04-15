package com.jobportal.backend.service;

import com.jobportal.backend.dto.job.request.CreateJobRequest;
import com.jobportal.backend.dto.job.request.UpdateJobRequest;
import com.jobportal.backend.dto.job.response.JobResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface JobService {

    JobResponse createJob(Long recruiterId, CreateJobRequest request);

    JobResponse getJobById(Long jobId);

    Page<JobResponse> getJobsByRecruiter(Long recruiterId, Pageable pageable);

    Page<JobResponse> getAllActiveJobs(Pageable pageable);

    JobResponse updateJob(Long jobId, UpdateJobRequest request, Long recruiterId);

    void deactivateJob(Long jobId, Long recruiterId);

    void activateJob(Long jobId, Long recruiterId);

    void deleteJob(Long jobId, Long recruiterId);


    Page<JobResponse> searchActiveJobs(String keyword, String location, String jobType,
                                       String experienceLevel, Boolean isRemote, Pageable pageable);
    Page<JobResponse> getActiveJobsByCompany(Long companyId, Pageable pageable);
    Page<JobResponse> getFeaturedJobs(Pageable pageable);

}