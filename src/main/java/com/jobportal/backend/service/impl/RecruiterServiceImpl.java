package com.jobportal.backend.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobportal.backend.dto.candidate.request.CertificationRequest;
import com.jobportal.backend.dto.candidate.request.EducationRequest;
import com.jobportal.backend.dto.candidate.request.ExperienceRequest;
import com.jobportal.backend.dto.candidate.request.LanguageRequest;
import com.jobportal.backend.dto.company.request.UpdateApplicationStatusRequest;
import com.jobportal.backend.dto.company.response.JobApplicationDetailResponse;
import com.jobportal.backend.dto.company.response.JobWithApplicationsResponse;
import com.jobportal.backend.dto.company.response.RecruiterDashboardResponse;
import com.jobportal.backend.dto.recruiter.response.CandidateProfileViewResponse;
import com.jobportal.backend.entity.*;
import com.jobportal.backend.enums.ApplicationStatus;
import com.jobportal.backend.exception.NotFoundException;
import com.jobportal.backend.exception.UnauthorizedAccessException;
import com.jobportal.backend.exception.ValidationException;
import com.jobportal.backend.repository.CandidateProfileRepository;
import com.jobportal.backend.repository.CompanyProfileRepository;
import com.jobportal.backend.repository.JobApplicationRepository;
import com.jobportal.backend.repository.JobRepository;
import com.jobportal.backend.service.RecruiterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecruiterServiceImpl implements RecruiterService {

    private final JobApplicationRepository jobApplicationRepository;
    private final JobRepository jobRepository;
    private final CompanyProfileRepository companyProfileRepository;
    private final CandidateProfileRepository candidateProfileRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public RecruiterDashboardResponse getDashboard(Long recruiterId) {
        log.info("Getting dashboard for recruiter ID: {}", recruiterId);

        // Get company profile
        CompanyProfile company = companyProfileRepository.findByRecruiterId(recruiterId)
                .orElseThrow(() -> new NotFoundException("Company profile not found"));

        // Get recruiter's jobs
        List<Job> recruiterJobs = jobRepository.findByCompanyRecruiterId(recruiterId);

        // Build dashboard response
        RecruiterDashboardResponse response = new RecruiterDashboardResponse();
        response.setCompanyId(company.getId());
        response.setCompanyName(company.getCompanyName());
        response.setIsCompanyVerified(company.isVerified());
        response.setTotalJobsPosted(recruiterJobs.size());
        response.setActiveJobs((int) recruiterJobs.stream().filter(Job::isActive).count());

        // Get applications statistics
        List<JobApplication> allApplications = jobApplicationRepository.findByRecruiterId(recruiterId);

        response.setTotalApplications(allApplications.size());
        response.setApplicationsThisMonth(
                jobApplicationRepository.countApplicationsThisMonth(recruiterId) != null ?
                        jobApplicationRepository.countApplicationsThisMonth(recruiterId) : 0
        );
        response.setNewApplications((int) allApplications.stream()
                .filter(app -> app.getStatus() == ApplicationStatus.APPLIED)
                .count());
        response.setShortlistedApplications((int) allApplications.stream()
                .filter(app -> app.getStatus() == ApplicationStatus.SHORTLISTED)
                .count());
        response.setInterviewScheduled((int) allApplications.stream()
                .filter(app -> app.getStatus() == ApplicationStatus.INTERVIEW_SCHEDULED)
                .count());
        response.setHiredCount((int) allApplications.stream()
                .filter(app -> app.getStatus() == ApplicationStatus.HIRED)
                .count());
        response.setRejectedCount((int) allApplications.stream()
                .filter(app -> app.getStatus() == ApplicationStatus.REJECTED)
                .count());

        // Get recent jobs with applications
        List<JobWithApplicationsResponse> recentJobs = recruiterJobs.stream()
                .limit(5) // Last 5 jobs
                .map(this::mapJobToWithApplicationsResponse)
                .collect(Collectors.toList());

        response.setRecentJobs(recentJobs);

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobWithApplicationsResponse> getAllJobsWithApplications(Long recruiterId) {
        log.debug("Getting all jobs with applications for recruiter ID: {}", recruiterId);

        List<Job> recruiterJobs = jobRepository.findByCompanyRecruiterId(recruiterId);

        return recruiterJobs.stream()
                .map(this::mapJobToWithApplicationsResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public JobWithApplicationsResponse getJobApplications(Long recruiterId, Long jobId) {
        log.debug("Getting applications for job ID: {} by recruiter ID: {}", jobId, recruiterId);

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new NotFoundException("Job not found"));

        // Check if job belongs to recruiter
        if (!job.getCompany().getRecruiter().getId().equals(recruiterId)) {
            throw new UnauthorizedAccessException("You are not authorized to view applications for this job");
        }

        return mapJobToWithApplicationsResponse(job);
    }

    @Override
    @Transactional(readOnly = true)
    public JobApplicationDetailResponse getApplicationDetail(Long recruiterId, Long applicationId) {
        log.debug("Getting application detail ID: {} by recruiter ID: {}", applicationId, recruiterId);

        JobApplication application = jobApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new NotFoundException("Application not found"));

        // Check if application belongs to recruiter's job
        if (!application.getJob().getCompany().getRecruiter().getId().equals(recruiterId)) {
            throw new UnauthorizedAccessException("You are not authorized to view this application");
        }

        return mapApplicationToDetailResponse(application);
    }

    @Override
    @Transactional
    public JobApplicationDetailResponse updateApplicationStatus(
            Long recruiterId,
            Long applicationId,
            UpdateApplicationStatusRequest request) {

        log.info("Updating application status ID: {} by recruiter ID: {}", applicationId, recruiterId);

        JobApplication application = jobApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new NotFoundException("Application not found"));

        // Check if application belongs to recruiter's job
        if (!application.getJob().getCompany().getRecruiter().getId().equals(recruiterId)) {
            throw new UnauthorizedAccessException("You are not authorized to update this application");
        }

        // Check if application is withdrawn
        if (application.isWithdrawn()) {
            throw new ValidationException("Cannot update status of a withdrawn application");
        }

        // Validate status transition
        if (!isValidStatusTransition(application.getStatus(), request.getStatus())) {
            throw new ValidationException("Invalid status transition from " +
                    application.getStatus() + " to " + request.getStatus());
        }

        // Update status
        application.setStatus(request.getStatus());
        application.setStatusChangedAt(LocalDateTime.now());

        // Add notes if provided
        if (request.getNotes() != null && !request.getNotes().trim().isEmpty()) {
            String currentNotes = application.getMatchNotes();
            application.setMatchNotes((currentNotes != null ? currentNotes + "\n" : "") +
                    "Recruiter Note [" + LocalDateTime.now() + "]: " + request.getNotes());
        }

        JobApplication updated = jobApplicationRepository.save(application);

        log.info("Application {} status updated to {}", applicationId, request.getStatus());

        return mapApplicationToDetailResponse(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobApplicationDetailResponse> getApplicationsByStatus(
            Long recruiterId,
            ApplicationStatus status) {

        log.debug("Getting applications with status {} for recruiter ID: {}", status, recruiterId);

        List<JobApplication> applications = jobApplicationRepository
                .findByRecruiterIdAndStatus(recruiterId, status);

        return applications.stream()
                .map(this::mapApplicationToDetailResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobApplicationDetailResponse> searchApplications(
            Long recruiterId,
            Long jobId,
            ApplicationStatus status,
            String keyword) {

        log.debug("Searching applications for recruiter ID: {}", recruiterId);

        List<JobApplication> allApplications = jobApplicationRepository
                .findByRecruiterId(recruiterId);

        return allApplications.stream()
                .filter(app -> filterApplication(app, jobId, status, keyword))
                .map(this::mapApplicationToDetailResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CandidateProfileViewResponse viewCandidateProfile(Long recruiterId, Long candidateId) {
        log.info("Recruiter {} viewing candidate profile {}", recruiterId, candidateId);

        // Verify recruiter has access to this candidate
        // A recruiter should only see candidates who applied to their jobs
        boolean hasAccess = jobApplicationRepository.existsByRecruiterIdAndCandidateId(recruiterId, candidateId);

        if (!hasAccess) {
            throw new UnauthorizedAccessException("You don't have access to view this candidate's profile");
        }

        CandidateProfile candidate = candidateProfileRepository.findById(candidateId)
                .orElseThrow(() -> new NotFoundException("Candidate profile not found"));

        return mapCandidateToProfileView(candidate);
    }

    private CandidateProfileViewResponse mapCandidateToProfileView(CandidateProfile candidate) {
        CandidateProfileViewResponse response = new CandidateProfileViewResponse();

        response.setCandidateId(candidate.getId());
        response.setFullName(candidate.getFullName());
        response.setEmail(candidate.getEmail());
        response.setPhone(candidate.getPhone());
        response.setProfilePictureUrl(candidate.getProfilePictureUrl());
        response.setHeadline(candidate.getHeadline());
        response.setSummary(candidate.getSummary());
        response.setTotalExperienceYears(candidate.getTotalExperienceYears());
        response.setCurrentSalary(String.valueOf(candidate.getCurrentSalary()));
        response.setExpectedSalary(String.valueOf(candidate.getExpectedSalary()));
        response.setSalaryCurrency(candidate.getSalaryCurrency());
        response.setIsActivelyLooking(candidate.isActivelyLooking());

        // Map experience
        try {
            if (candidate.getExperienceJson() != null) {
                List<ExperienceRequest> experiences = objectMapper.readValue(
                        candidate.getExperienceJson(),
                        new TypeReference<List<ExperienceRequest>>() {}
                );
                response.setExperiences(experiences.stream()
                        .map(this::mapExperienceToView)
                        .collect(Collectors.toList()));
            }
        } catch (JsonProcessingException e) {
            log.error("Error parsing experience JSON", e);
        }

        // Map education
        try {
            if (candidate.getEducationJson() != null) {
                List<EducationRequest> educations = objectMapper.readValue(
                        candidate.getEducationJson(),
                        new TypeReference<List<EducationRequest>>() {}
                );
                response.setEducations(educations.stream()
                        .map(this::mapEducationToView)
                        .collect(Collectors.toList()));
            }
        } catch (JsonProcessingException e) {
            log.error("Error parsing education JSON", e);
        }

        // Map skills
        response.setSkills(candidate.getSkillsList().stream()
                .map(this::mapSkillToView)
                .collect(Collectors.toList()));

        // Map certifications
        try {
            if (candidate.getCertificationsJson() != null) {
                List<CertificationRequest> certifications = objectMapper.readValue(
                        candidate.getCertificationsJson(),
                        new TypeReference<List<CertificationRequest>>() {}
                );
                response.setCertifications(certifications.stream()
                        .map(this::mapCertificationToView)
                        .collect(Collectors.toList()));
            }
        } catch (JsonProcessingException e) {
            log.error("Error parsing certifications JSON", e);
        }

        // Map languages
        try {
            if (candidate.getLanguagesJson() != null) {
                List<LanguageRequest> languages = objectMapper.readValue(
                        candidate.getLanguagesJson(),
                        new TypeReference<List<LanguageRequest>>() {}
                );
                response.setLanguages(languages.stream()
                        .map(this::mapLanguageToView)
                        .collect(Collectors.toList()));
            }
        } catch (JsonProcessingException e) {
            log.error("Error parsing languages JSON", e);
        }

        response.setResumeUrl(candidate.getResumeUrl());
        response.setResumeFileName(candidate.getResumeFileName());
        response.setPreferredJobTypes(candidate.getPreferredJobTypesList());
        response.setPreferredLocations(candidate.getPreferredLocationsList());

        return response;
    }

    // Add these mapping helper methods
    private CandidateProfileViewResponse.ExperienceView mapExperienceToView(ExperienceRequest exp) {
        CandidateProfileViewResponse.ExperienceView view = new CandidateProfileViewResponse.ExperienceView();
        view.setCompany(exp.getCompany());
        view.setPosition(exp.getPosition());
        view.setEmploymentType(exp.getEmploymentType());
        view.setLocation(exp.getLocation());
        view.setIsCurrent(exp.getIsCurrent());
        view.setStartDate(exp.getStartDate());
        view.setEndDate(exp.getEndDate());
        view.setDescription(exp.getDescription());
        view.setAchievements(Collections.singletonList(exp.getAchievements()));
        view.setSkillsUsed(exp.getSkillsUsed());
        return view;
    }

    private CandidateProfileViewResponse.EducationView mapEducationToView(EducationRequest edu) {
        CandidateProfileViewResponse.EducationView view = new CandidateProfileViewResponse.EducationView();
        view.setInstitution(edu.getInstitution());
        view.setDegree(edu.getDegree());
        view.setFieldOfStudy(edu.getFieldOfStudy());
        view.setGrade(edu.getGrade());
        view.setStartDate(edu.getStartDate());
        view.setEndDate(edu.getEndDate());
        view.setIsCurrent(edu.getIsCurrent());
        view.setDescription(edu.getDescription());
        return view;
    }

    private CandidateProfileViewResponse.SkillView mapSkillToView(String skillName) {
        CandidateProfileViewResponse.SkillView view = new CandidateProfileViewResponse.SkillView();
        view.setName(skillName);
        // You might want to fetch years of experience and proficiency from somewhere else
        return view;
    }

    private CandidateProfileViewResponse.CertificationView mapCertificationToView(CertificationRequest cert) {
        CandidateProfileViewResponse.CertificationView view = new CandidateProfileViewResponse.CertificationView();
        view.setName(cert.getName());
        view.setIssuingOrganization(cert.getIssuingOrganization());
        view.setIssueDate(cert.getIssueDate());
        view.setExpirationDate(cert.getExpirationDate());
        return view;
    }

    private CandidateProfileViewResponse.LanguageView mapLanguageToView(LanguageRequest lang) {
        CandidateProfileViewResponse.LanguageView view = new CandidateProfileViewResponse.LanguageView();
        view.setLanguage(lang.getLanguage());
        view.setProficiency(lang.getProficiency());
        return view;
    }

    // Helper Methods
    private JobWithApplicationsResponse mapJobToWithApplicationsResponse(Job job) {
        JobWithApplicationsResponse response = new JobWithApplicationsResponse();
        response.setJobId(job.getId());
        response.setJobTitle(job.getTitle());
        response.setJobType(job.getJobType().name());
        response.setLocation(job.getLocation());
        response.setIsRemote(job.isRemote());
        response.setIsActive(job.isActive());
        response.setTotalApplications(job.getTotalApplications());

        // Filter applications for this job
        List<JobApplication> jobApplications = job.getJobApplications();

        long activeApps = jobApplications.stream()
                .filter(app -> !app.isWithdrawn())
                .count();
        long withdrawnApps = jobApplications.stream()
                .filter(JobApplication::isWithdrawn)
                .count();

        response.setActiveApplications((int) activeApps);
        response.setWithdrawnApplications((int) withdrawnApps);

        // Map application details
        List<JobApplicationDetailResponse> applicationResponses = jobApplications.stream()
                .filter(app -> !app.isWithdrawn()) // Only show active applications
                .map(this::mapApplicationToDetailResponse)
                .collect(Collectors.toList());

        response.setApplications(applicationResponses);

        return response;
    }

    private JobApplicationDetailResponse mapApplicationToDetailResponse(JobApplication application) {
        JobApplicationDetailResponse response = new JobApplicationDetailResponse();
        response.setId(application.getId());
        response.setStatus(application.getStatus());
        response.setAppliedAt(application.getAppliedAt());
        response.setStatusChangedAt(application.getStatusChangedAt());
        response.setCoverLetter(application.getCoverLetter());
        response.setMatchScore(application.getMatchScore());
        response.setMatchNotes(application.getMatchNotes());
        response.setIsWithdrawn(application.isWithdrawn());
        response.setIsFavorite(application.isFavorite());
        response.setCreatedAt(application.getCreatedAt());
        response.setUpdatedAt(application.getUpdatedAt());

        // Candidate details
        CandidateProfile candidate = application.getCandidate();
        if (candidate != null) {
            response.setCandidateId(candidate.getId());
            response.setCandidateName(candidate.getFullName());
            response.setCandidateEmail(candidate.getEmail());
            response.setCandidateProfilePicture(candidate.getProfilePictureUrl());
            response.setCandidateHeadline(candidate.getHeadline());
            response.setCandidateTotalExperience(candidate.getTotalExperienceYears());
        }

        return response;
    }

    private boolean isValidStatusTransition(ApplicationStatus current, ApplicationStatus next) {
        // Define valid status transitions
        switch (current) {
            case APPLIED:
                return next == ApplicationStatus.VIEWED ||
                        next == ApplicationStatus.SHORTLISTED ||
                        next == ApplicationStatus.REJECTED;
            case VIEWED:
                return next == ApplicationStatus.SHORTLISTED ||
                        next == ApplicationStatus.REJECTED;
            case SHORTLISTED:
                return next == ApplicationStatus.INTERVIEW_SCHEDULED ||
                        next == ApplicationStatus.REJECTED;
            case INTERVIEW_SCHEDULED:
                return next == ApplicationStatus.INTERVIEW_COMPLETED ||
                        next == ApplicationStatus.REJECTED;
            case INTERVIEW_COMPLETED:
                return next == ApplicationStatus.HIRED ||
                        next == ApplicationStatus.REJECTED;
            case HIRED:
                return false; // Cannot change from HIRED
            case REJECTED:
                return false; // Cannot change from REJECTED
            case WITHDRAWN:
                return false; // Cannot change from WITHDRAWN
            default:
                return false;
        }
    }

    private boolean filterApplication(JobApplication application,
                                      Long jobId,
                                      ApplicationStatus status,
                                      String keyword) {
        // Filter by job ID
        if (jobId != null && !application.getJob().getId().equals(jobId)) {
            return false;
        }

        // Filter by status
        if (status != null && application.getStatus() != status) {
            return false;
        }

        // Filter by keyword
        if (keyword != null && !keyword.trim().isEmpty()) {
            String searchTerm = keyword.toLowerCase().trim();
            CandidateProfile candidate = application.getCandidate();

            // Search in candidate name, email, headline
            boolean matchesCandidate = (candidate.getFullName() != null &&
                    candidate.getFullName().toLowerCase().contains(searchTerm)) ||
                    (candidate.getEmail() != null &&
                            candidate.getEmail().toLowerCase().contains(searchTerm)) ||
                    (candidate.getHeadline() != null &&
                            candidate.getHeadline().toLowerCase().contains(searchTerm));

            // Search in cover letter
            boolean matchesCoverLetter = application.getCoverLetter() != null &&
                    application.getCoverLetter().toLowerCase().contains(searchTerm);

            // Search in job title
            boolean matchesJobTitle = application.getJob().getTitle() != null &&
                    application.getJob().getTitle().toLowerCase().contains(searchTerm);

            return matchesCandidate || matchesCoverLetter || matchesJobTitle;
        }

        return true;
    }
}