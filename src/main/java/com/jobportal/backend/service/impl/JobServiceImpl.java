package com.jobportal.backend.service.impl;

import com.jobportal.backend.dto.job.request.CreateJobRequest;
import com.jobportal.backend.dto.job.request.UpdateJobRequest;
import com.jobportal.backend.dto.job.response.JobResponse;
import com.jobportal.backend.entity.*;
import com.jobportal.backend.enums.ExperienceLevel;
import com.jobportal.backend.enums.JobType;
import com.jobportal.backend.enums.SalaryType;
import com.jobportal.backend.exception.ResourceNotFoundException;
import com.jobportal.backend.exception.UnauthorizedAccessException;
import com.jobportal.backend.exception.ValidationException;
import com.jobportal.backend.repository.CompanyProfileRepository;
import com.jobportal.backend.repository.JobDescriptionRepository;
import com.jobportal.backend.repository.JobRepository;
import com.jobportal.backend.service.JobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.jobportal.backend.exception.BadRequestException;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobServiceImpl implements JobService {

    private final JobRepository jobRepository;
    private final CompanyProfileRepository companyProfileRepository;
    private final JobDescriptionRepository jobDescriptionRepository;

    @Override
    @Transactional
    public JobResponse createJob(Long recruiterId, CreateJobRequest request) {
        log.info("Creating job for recruiter ID: {}", recruiterId);

        // Get recruiter's company profile
        CompanyProfile company = companyProfileRepository.findByRecruiterId(recruiterId)
                .orElseThrow(() -> new ResourceNotFoundException("Company profile not found"));

        // Check if company can post jobs
        if (!company.isProfileComplete()) {
            throw new ValidationException("Company profile must be complete to post jobs");
        }

        if (!company.isActive()) {
            throw new ValidationException("Company profile is not active");
        }

        if (!company.isVerified()) {
            throw new ValidationException("Company must be verified by admin to post jobs");
        }

        // Validate salary range
        if (request.getSalaryMin() != null && request.getSalaryMax() != null
                && request.getSalaryMin().compareTo(request.getSalaryMax()) > 0) {
            throw new ValidationException("Minimum salary cannot be greater than maximum salary");
        }

        // Create job
        Job job = new Job();
        job.setTitle(request.getTitle());
        job.setJobType(request.getJobType());
        job.setExperienceLevel(request.getExperienceLevel());
        job.setLocation(request.getLocation());
        job.setSalaryMin(request.getSalaryMin());
        job.setSalaryMax(request.getSalaryMax());
        job.setSalaryCurrency(request.getSalaryCurrency() != null ? request.getSalaryCurrency() : "NPR");
        job.setSalaryType(request.getSalaryType() != null ? request.getSalaryType() : SalaryType.MONTHLY);
        job.setRemote(request.getIsRemote() != null ? request.getIsRemote() : false);
        job.setApplicationDeadline(request.getApplicationDeadline());
        job.setCompany(company);
        job.setPublishedAt(LocalDateTime.now());

        // SET SKILLS - Using the helper method
        if (request.getSkills() != null && !request.getSkills().isEmpty()) {
            job.setRequiredSkillsList(request.getSkills());
            log.info("Added {} required skills to job", request.getSkills().size());
        }

        Job savedJob = jobRepository.save(job);

        // Create job description
        JobDescription jobDescription = new JobDescription();
        jobDescription.setOverview(request.getOverview());
        jobDescription.setResponsibilities(request.getResponsibilities());
        jobDescription.setRequirements(request.getRequirements());
        jobDescription.setNiceToHave(request.getNiceToHave());
        jobDescription.setBenefits(request.getBenefits());
        jobDescription.setApplicationInstructions(request.getApplicationInstructions());
        jobDescription.setJob(savedJob);

        jobDescriptionRepository.save(jobDescription);

        savedJob.setJobDescription(jobDescription);

        log.info("Job created with ID: {}", savedJob.getId());

        return mapToResponse(savedJob);
    }

    @Override
    @Transactional(readOnly = true)
    public JobResponse getJobById(Long jobId) {
        log.debug("Getting job by ID: {}", jobId);

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with ID: " + jobId));

        // Check if job's company is visible to public
        if (!job.getCompany().isVisibleToPublic()) {
            throw new ResourceNotFoundException("Job not found or not visible");
        }

        return mapToResponse(job);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<JobResponse> getJobsByRecruiter(Long recruiterId, Pageable pageable) {
        log.debug("Getting jobs for recruiter ID: {}", recruiterId);

        CompanyProfile company = companyProfileRepository.findByRecruiterId(recruiterId)
                .orElseThrow(() -> new ResourceNotFoundException("Company profile not found"));

        return jobRepository.findByCompany(company, pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<JobResponse> getAllActiveJobs(Pageable pageable) {
        log.debug("Getting all active jobs");

        // Only get jobs from active and verified companies
        return jobRepository.findByIsActiveAndCompanyIsActiveAndCompanyIsVerified(true, true, true, pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional
    public JobResponse updateJob(Long jobId, UpdateJobRequest request, Long recruiterId) {
        log.info("Updating job ID: {}", jobId);

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));

        // Check if recruiter owns this job
        if (!isJobOwner(jobId, recruiterId)) {
            throw new UnauthorizedAccessException("You are not authorized to update this job");
        }

        // Check if company can still post jobs
        if (!job.getCompany().canPostJobs()) {
            throw new ValidationException("Company cannot post jobs (may be inactive or unverified)");
        }

        // Validate salary
        if (request.getSalaryMin() != null && request.getSalaryMax() != null
                && request.getSalaryMin().compareTo(request.getSalaryMax()) > 0) {
            throw new ValidationException("Minimum salary cannot be greater than maximum salary");
        }

        // Update fields if provided
        if (request.getTitle() != null) job.setTitle(request.getTitle());
        if (request.getJobType() != null) job.setJobType(request.getJobType());
        if (request.getExperienceLevel() != null) job.setExperienceLevel(request.getExperienceLevel());
        if (request.getLocation() != null) job.setLocation(request.getLocation());
        if (request.getSalaryMin() != null) job.setSalaryMin(request.getSalaryMin());
        if (request.getSalaryMax() != null) job.setSalaryMax(request.getSalaryMax());
        if (request.getSalaryCurrency() != null) job.setSalaryCurrency(request.getSalaryCurrency());
        if (request.getSalaryType() != null) job.setSalaryType(request.getSalaryType());
        if (request.getIsRemote() != null) job.setRemote(request.getIsRemote());
        if (request.getApplicationDeadline() != null) job.setApplicationDeadline(request.getApplicationDeadline());

        // UPDATE SKILLS
        if (request.getSkills() != null) {
            job.setRequiredSkillsList(request.getSkills());
            log.info("Updated skills for job {}: {} skills", jobId, request.getSkills().size());
        }

        // Update job description if provided
        if (job.getJobDescription() != null) {
            JobDescription description = job.getJobDescription();
            if (request.getOverview() != null) description.setOverview(request.getOverview());
            if (request.getResponsibilities() != null) description.setResponsibilities(request.getResponsibilities());
            if (request.getRequirements() != null) description.setRequirements(request.getRequirements());
            if (request.getNiceToHave() != null) description.setNiceToHave(request.getNiceToHave());
            if (request.getBenefits() != null) description.setBenefits(request.getBenefits());
            if (request.getApplicationInstructions() != null) description.setApplicationInstructions(request.getApplicationInstructions());
        }

        Job updatedJob = jobRepository.save(job);

        log.info("Job updated successfully: {}", jobId);
        return mapToResponse(updatedJob);
    }

    @Override
    @Transactional
    public void deactivateJob(Long jobId, Long recruiterId) {
        log.info("Deactivating job ID: {}", jobId);

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));

        if (!isJobOwner(jobId, recruiterId)) {
            throw new UnauthorizedAccessException("You are not authorized to deactivate this job");
        }

        job.setActive(false);
        jobRepository.save(job);

        log.info("Job deactivated: {}", jobId);
    }

    @Override
    @Transactional
    public void activateJob(Long jobId, Long recruiterId) {
        log.info("Activating job ID: {}", jobId);

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));

        if (!isJobOwner(jobId, recruiterId)) {
            throw new UnauthorizedAccessException("You are not authorized to activate this job");
        }

        // Check if company can still post jobs
        if (!job.getCompany().canPostJobs()) {
            throw new ValidationException("Cannot activate job because company cannot post jobs");
        }

        job.setActive(true);
        jobRepository.save(job);

        log.info("Job activated: {}", jobId);
    }

    @Override
    @Transactional
    public void deleteJob(Long jobId, Long recruiterId) {
        log.info("Deleting job ID: {}", jobId);

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));

        if (!isJobOwner(jobId, recruiterId)) {
            throw new UnauthorizedAccessException("You are not authorized to delete this job");
        }

        jobRepository.delete(job);

        log.info("Job deleted: {}", jobId);
    }

    // Helper method to check job ownership
    private boolean isJobOwner(Long jobId, Long recruiterId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));

        return job.getCompany().getRecruiter().getId().equals(recruiterId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<JobResponse> searchActiveJobs(String keyword, String location, String jobType,
                                              String experienceLevel, Boolean isRemote, Pageable pageable) {
        log.debug("Searching active jobs with filters");

        String normalizedKeyword = (keyword != null) ? keyword.trim() : null;
        String normalizedLocation = (location != null) ? location.trim() : null;

        JobType jobTypeEnum = null;
        if (jobType != null && !jobType.trim().isEmpty()) {
            try {
                jobTypeEnum = JobType.valueOf(jobType.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid job type: " + jobType);
            }
        }

        ExperienceLevel experienceLevelEnum = null;
        if (experienceLevel != null && !experienceLevel.trim().isEmpty()) {
            try {
                experienceLevelEnum = ExperienceLevel.valueOf(experienceLevel.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid experience level: " + experienceLevel);
            }
        }

        if (normalizedKeyword != null && !normalizedKeyword.isEmpty()) {
            return jobRepository.searchActiveJobsByKeyword(
                            normalizedKeyword,
                            normalizedLocation,
                            jobTypeEnum,
                            experienceLevelEnum,
                            isRemote,
                            pageable)
                    .map(this::mapToResponse);
        } else {
            return jobRepository.findActiveJobsWithFilters(
                            normalizedLocation,
                            jobTypeEnum,
                            experienceLevelEnum,
                            isRemote,
                            pageable)
                    .map(this::mapToResponse);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<JobResponse> getActiveJobsByCompany(Long companyId, Pageable pageable) {
        log.debug("Getting active jobs for company ID: {}", companyId);

        return jobRepository.findByCompanyIdAndIsActiveAndCompanyIsActiveAndCompanyIsVerified(
                        companyId, true, true, true, pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<JobResponse> getFeaturedJobs(Pageable pageable) {
        log.debug("Getting featured jobs");

        return jobRepository.findByIsFeaturedAndIsActiveAndCompanyIsActiveAndCompanyIsVerified(
                        true, true, true, true, pageable)
                .map(this::mapToResponse);
    }

    // Map Job entity to JobResponse DTO
    // Update mapToResponse to include skills
    private JobResponse mapToResponse(Job job) {
        JobResponse response = new JobResponse();
        response.setId(job.getId());
        response.setTitle(job.getTitle());
        response.setJobType(job.getJobType());
        response.setExperienceLevel(job.getExperienceLevel());
        response.setLocation(job.getLocation());
        response.setSalaryMin(job.getSalaryMin());
        response.setSalaryMax(job.getSalaryMax());
        response.setSalaryCurrency(job.getSalaryCurrency());
        response.setSalaryType(job.getSalaryType());
        response.setRemote(job.isRemote());
        response.setActive(job.isActive());
        response.setFeatured(job.isFeatured());
        response.setTotalApplications(job.getTotalApplications());
        response.setApplicationDeadline(job.getApplicationDeadline());
        response.setCreatedAt(job.getCreatedAt());
        response.setUpdatedAt(job.getUpdatedAt());
        response.setPublishedAt(job.getPublishedAt());

        // ADD SKILLS TO RESPONSE
        response.setRequiredSkills(job.getRequiredSkillsList());

        // Set job description
        if (job.getJobDescription() != null) {
            response.setOverview(job.getJobDescription().getOverview());
            response.setResponsibilities(job.getJobDescription().getResponsibilities());
            response.setRequirements(job.getJobDescription().getRequirements());
            response.setNiceToHave(job.getJobDescription().getNiceToHave());
            response.setBenefits(job.getJobDescription().getBenefits());
            response.setApplicationInstructions(job.getJobDescription().getApplicationInstructions());
        }

        // Set company info
        if (job.getCompany() != null) {
            response.setCompanyId(job.getCompany().getId());
            response.setCompanyName(job.getCompany().getCompanyName());
            response.setCompanyLogo(job.getCompany().getLogoUrl());
            if (job.getCompany().getIndustry() != null) {
                response.setIndustryName(job.getCompany().getIndustry().getName());
            }
        }

        return response;
    }
}