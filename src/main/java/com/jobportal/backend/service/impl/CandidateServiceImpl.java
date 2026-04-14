package com.jobportal.backend.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobportal.backend.dto.ProfileHealth;
import com.jobportal.backend.dto.candidate.RecentApplication;
import com.jobportal.backend.dto.candidate.RecommendedJob;
import com.jobportal.backend.dto.candidate.request.*;
import com.jobportal.backend.dto.candidate.response.*;
import com.jobportal.backend.entity.*;
import com.jobportal.backend.enums.*;
import com.jobportal.backend.exception.BadRequestException;
import com.jobportal.backend.exception.CustomAccessDeniedException;
import com.jobportal.backend.exception.NotFoundException;
import com.jobportal.backend.exception.UnauthorizedAccessException;
import com.jobportal.backend.repository.*;
import com.jobportal.backend.service.CandidateService;
import com.jobportal.backend.service.MatchScoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class CandidateServiceImpl implements CandidateService {

    private final CandidateProfileRepository candidateProfileRepository;
    private final UserRepository userRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final JobRepository jobRepository;
    private final ObjectMapper objectMapper;
    private final MatchScoreService matchScoreService;

    @Override
    @Transactional
    public CandidateProfileResponse createOrUpdateProfile(Long userId, CandidateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (!user.hasRole(RoleEnum.CANDIDATE)) {
            throw new CustomAccessDeniedException("Only candidates can create profiles");
        }

        if (!user.isActive()) {
            throw new CustomAccessDeniedException("Your account is not active");
        }

        if (!user.isEmailVerified()) {
            throw new CustomAccessDeniedException("Please verify your email first");
        }

        CandidateProfile profile = candidateProfileRepository.findByUserId(userId)
                .orElseGet(() -> {
                    CandidateProfile newProfile = new CandidateProfile();
                    newProfile.setUser(user);
                    newProfile.setEmail(request.getEmail());
                    return newProfile;
                });

        // Check if email is being changed and if it's already taken
        if (!request.getEmail().equals(profile.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new BadRequestException("Email is already registered");
            }
            if (candidateProfileRepository.existsByEmail(request.getEmail())) {
                throw new BadRequestException("Email is already used by another candidate");
            }
        }

        updateProfileFromRequest(profile, request);
        CandidateProfile saved = candidateProfileRepository.save(profile);

        log.info("Profile saved for user: {}, profile ID: {}", userId, saved.getId());
        return mapToProfileResponse(saved);
    }

    private void updateProfileFromRequest(CandidateProfile profile, CandidateProfileRequest request) {
        profile.setFirstName(request.getFirstName());
        profile.setLastName(request.getLastName());
        profile.setEmail(request.getEmail());
        profile.setPhone(request.getPhone());
        profile.setDateOfBirth(request.getDateOfBirth());
        profile.setGender(request.getGender());
        profile.setAddress(request.getAddress());
        profile.setCity(request.getCity());
        profile.setCountry(request.getCountry());
        profile.setProfilePictureUrl(request.getProfilePictureUrl());
        profile.setHeadline(request.getHeadline());
        profile.setSummary(request.getSummary());
        profile.setTotalExperienceYears(request.getTotalExperienceYears());
        profile.setCurrentSalary(request.getCurrentSalary());
        profile.setExpectedSalary(request.getExpectedSalary());

        if (request.getSalaryCurrency() != null) {
            profile.setSalaryCurrency(request.getSalaryCurrency());
        }

        if (request.getNoticePeriod() != null) {
            profile.setNoticePeriod(request.getNoticePeriod());
        }

        try {
            if (request.getEducation() != null) {
                profile.setEducationJson(objectMapper.writeValueAsString(request.getEducation()));
            }
            if (request.getExperience() != null) {
                profile.setExperienceJson(objectMapper.writeValueAsString(request.getExperience()));
            }
            if (request.getCertifications() != null) {
                profile.setCertificationsJson(objectMapper.writeValueAsString(request.getCertifications()));
            }
            if (request.getLanguages() != null) {
                profile.setLanguagesJson(objectMapper.writeValueAsString(request.getLanguages()));
            }
        } catch (JsonProcessingException e) {
            log.error("Error converting to JSON", e);
        }

        if (request.getSkills() != null) {
            profile.setSkillsList(request.getSkills());
        }

        if (request.getPreferredJobTypes() != null) {
            profile.setPreferredJobTypesList(request.getPreferredJobTypes());
        }
        if (request.getPreferredLocations() != null) {
            profile.setPreferredLocationsList(request.getPreferredLocations());
        }
        if (request.getPreferredIndustries() != null) {
            profile.setPreferredIndustriesJson("[\"" + String.join("\",\"", request.getPreferredIndustries()) + "\"]");
        }

        if (request.getRemotePreference() != null) {
            profile.setRemotePreference(request.getRemotePreference());
        }

        if (request.getProfileVisibility() != null) {
            profile.setProfileVisibility(request.getProfileVisibility());
        }
        if (request.getIsActivelyLooking() != null) {
            profile.setActivelyLooking(request.getIsActivelyLooking());
        }

        profile.setAvailableFrom(request.getAvailableFrom());
    }

    @Override
    @Transactional(readOnly = true)
    public CandidateProfileResponse getProfile(Long userId) {
        CandidateProfile profile = candidateProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Candidate profile not found"));

        if (!profile.getUser().isActive()) {
            throw new BadRequestException("Your account is not active");
        }

        return mapToProfileResponse(profile);
    }

    @Override
    @Transactional
    public CandidateProfileResponse uploadResume(Long userId, ResumeUploadRequest request) {
        CandidateProfile profile = candidateProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Profile not found"));

        if (!profile.getUser().isActive()) {
            throw new BadRequestException("Your account is not active");
        }

        profile.setResumeUrl(request.getResumeUrl());
        profile.setResumeFileName(request.getFileName());
        profile.setResumeOriginalName(request.getOriginalFileName());
        profile.setResumeUploaded(true);

        CandidateProfile saved = candidateProfileRepository.save(profile);
        log.info("Resume uploaded for user: {}, file: {}", userId, request.getFileName());

        return mapToProfileResponse(saved);
    }

    @Override
    @Transactional
    public CandidateProfileResponse updateVisibility(Long userId, ProfileVisibility visibility) {
        CandidateProfile profile = candidateProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Profile not found"));

        if (!profile.getUser().isActive()) {
            throw new BadRequestException("Your account is not active");
        }

        profile.setProfileVisibility(visibility);
        CandidateProfile saved = candidateProfileRepository.save(profile);

        log.info("Updated visibility to {} for user: {}", visibility, userId);
        return mapToProfileResponse(saved);
    }

    @Override
    @Transactional
    public CandidateProfileResponse updateActiveStatus(Long userId, boolean isActivelyLooking) {
        CandidateProfile profile = candidateProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Profile not found"));

        if (!profile.getUser().isActive()) {
            throw new BadRequestException("Your account is not active");
        }

        profile.setActivelyLooking(isActivelyLooking);
        CandidateProfile saved = candidateProfileRepository.save(profile);

        log.info("Updated active status to {} for user: {}", isActivelyLooking, userId);
        return mapToProfileResponse(saved);
    }

    @Override
    @Transactional
    public void deleteProfile(Long userId) {
        CandidateProfile profile = candidateProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Profile not found"));

        profile.setActivelyLooking(false);
        profile.setProfileVisibility(ProfileVisibility.HIDDEN);
        candidateProfileRepository.save(profile);

        log.info("Profile hidden for user: {}", userId);
    }

    @Override
    @Transactional(readOnly = true)
    public CandidateProfileResponse getProfileForApplication(Long userId) {
        CandidateProfile profile = candidateProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Profile not found"));

        List<String> eligibilityReasons = new ArrayList<>();

        if (!profile.getUser().isActive()) {
            eligibilityReasons.add("Your account is not active");
        }
        if (!profile.getUser().isEmailVerified()) {
            eligibilityReasons.add("Please verify your email");
        }
        if (!profile.isProfileComplete()) {
            eligibilityReasons.add("Complete your profile to at least 80%");
        }
        // CV is optional - removed from eligibility check
        // Actively looking is optional - removed from eligibility check

        CandidateProfileResponse response = mapToProfileResponse(profile);
        response.setEligibilityReasons(eligibilityReasons);

        return response;
    }

    @Override
    @Transactional
    public JobApplicationResponse applyForJob(Long userId, JobApplicationRequest request) {
        CandidateProfile candidate = candidateProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Candidate profile not found"));

        // Validate candidate eligibility
        validateCandidateEligibility(candidate);

        Job job = jobRepository.findById(request.getJobId())
                .orElseThrow(() -> new NotFoundException("Job not found"));

        // Validate job eligibility
        validateJobEligibility(job, candidate);

        // Check for duplicate application
        if (jobApplicationRepository.existsByJobIdAndCandidateId(job.getId(), candidate.getId())) {
            throw new BadRequestException("You have already applied for this job");
        }

        // Create and save application
        JobApplication application = createJobApplication(job, candidate, request);
        JobApplication saved = jobApplicationRepository.save(application);

        // Update job statistics
        updateJobApplicationCount(job);

        log.info("Candidate {} applied for job {} with match score {}",
                candidate.getId(), job.getId(), saved.getMatchScore());

        return mapToApplicationResponse(saved);
    }

    private void validateCandidateEligibility(CandidateProfile candidate) {
        User user = candidate.getUser();

        if (!user.isActive()) {
            throw new BadRequestException("Your account is not active");
        }
        if (!user.isEmailVerified()) {
            throw new BadRequestException("Please verify your email to apply for jobs");
        }
        if (!candidate.isProfileComplete()) {
            throw new BadRequestException(
                    "Complete your profile to at least 80% to apply for jobs. Current completion: " +
                            candidate.getCompletionPercentage() + "%"
            );
        }
        // CV is optional - removed check
        // Actively looking is optional - removed check
    }

    private void validateJobEligibility(Job job, CandidateProfile candidate) {
        if (!job.isActive()) {
            throw new BadRequestException("This job is no longer active");
        }

        if (!job.isApplicationOpen()) {
            throw new BadRequestException("Application deadline has passed or job is closed");
        }

        if (!job.getCompany().isActive()) {
            throw new BadRequestException("Company is not active");
        }

        if (!job.getCompany().isVerified()) {
            throw new BadRequestException("Company is not verified");
        }

        if (!job.canApply(candidate)) {
            throw new BadRequestException("You are not eligible to apply for this job");
        }
    }

    private JobApplication createJobApplication(Job job, CandidateProfile candidate, JobApplicationRequest request) {
        JobApplication application = new JobApplication();
        application.setJob(job);
        application.setCandidate(candidate);
        application.setCoverLetter(request.getCoverLetter());

        // No answers field since there are no questions

        // Calculate match score using MatchScoreService
        int matchScore = matchScoreService.calculateMatchScore(candidate, job);
        application.setMatchScore(matchScore);
        application.setMatchNotes(matchScoreService.generateMatchNotes(candidate, job, matchScore));

        return application;
    }

    private void updateJobApplicationCount(Job job) {
        job.setTotalApplications(job.getTotalApplications() + 1);
        jobRepository.save(job);
    }

    @Override
    @Transactional
    public JobApplicationResponse withdrawApplication(Long userId, Long applicationId) {
        JobApplication application = jobApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new NotFoundException("Application not found"));

        // Check ownership
        if (!application.getCandidate().getUser().getId().equals(userId)) {
            throw new UnauthorizedAccessException("You can only withdraw your own applications");
        }

        // Check if already withdrawn
        if (application.isWithdrawn()) {
            throw new BadRequestException("Application already withdrawn");
        }

        // Check if can be withdrawn (not hired/rejected)
        if (!canWithdrawApplication(application)) {
            throw new BadRequestException("Cannot withdraw application in current status");
        }

        // Withdraw application
        application.setWithdrawn(true);
        application.setStatus(ApplicationStatus.WITHDRAWN);

        JobApplication saved = jobApplicationRepository.save(application);
        log.info("Application {} withdrawn by user {}", applicationId, userId);

        return mapToApplicationResponse(saved);
    }

    private boolean canWithdrawApplication(JobApplication application) {
        return application.getStatus() != ApplicationStatus.HIRED &&
                application.getStatus() != ApplicationStatus.REJECTED;
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobApplicationResponse> getApplications(Long userId) {
        List<JobApplication> applications = jobApplicationRepository.findByUserId(userId);
        return applications.stream()
                .map(this::mapToApplicationResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<JobApplicationResponse> getApplications(Long userId, Pageable pageable) {
        CandidateProfile candidate = candidateProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Candidate profile not found"));

        Page<JobApplication> applications = jobApplicationRepository.findByCandidateId(candidate.getId(), pageable);
        return applications.map(this::mapToApplicationResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public JobApplicationResponse getApplication(Long userId, Long applicationId) {
        JobApplication application = jobApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new NotFoundException("Application not found"));

        if (!application.getCandidate().getUser().getId().equals(userId)) {
            throw new UnauthorizedAccessException("You can only view your own applications");
        }

        return mapToApplicationResponse(application);
    }

    @Override
    @Transactional(readOnly = true)
    public CandidateDashboardResponse getDashboard(Long userId) {
        CandidateProfile candidate = candidateProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Profile not found"));

        if (!candidate.getUser().isActive()) {
            throw new BadRequestException("Your account is not active");
        }

        CandidateDashboardResponse response = new CandidateDashboardResponse();
        response.setProfile(mapToProfileResponse(candidate));

        List<JobApplication> applications = jobApplicationRepository.findByUserId(userId);

        response.setTotalApplications(applications.size());
        response.setPendingApplications((int) applications.stream()
                .filter(app -> app.getStatus() == ApplicationStatus.APPLIED ||
                        app.getStatus() == ApplicationStatus.VIEWED)
                .count());
        response.setInterviewApplications((int) applications.stream()
                .filter(app -> app.getStatus() == ApplicationStatus.INTERVIEW_SCHEDULED ||
                        app.getStatus() == ApplicationStatus.INTERVIEW_COMPLETED ||
                        app.getStatus() == ApplicationStatus.SHORTLISTED)
                .count());
        response.setRejectedApplications((int) applications.stream()
                .filter(app -> app.getStatus() == ApplicationStatus.REJECTED)
                .count());
        response.setHiredApplications((int) applications.stream()
                .filter(app -> app.getStatus() == ApplicationStatus.HIRED)
                .count());

        List<JobApplication> recentApps = jobApplicationRepository
                .findRecentByCandidateId(candidate.getId(), Pageable.ofSize(5));

        List<RecentApplication> recentApplications = recentApps.stream()
                .map(this::mapToRecentApplication)
                .collect(Collectors.toList());
        response.setRecentApplications(recentApplications);

        response.setProfileHealth(getProfileHealth(candidate));
        response.setRecommendedJobs(getRecommendedJobs(candidate));

        return response;
    }

    private ProfileHealth getProfileHealth(CandidateProfile candidate) {
        ProfileHealth health = new ProfileHealth();

        health.setCompletionPercentage(candidate.getCompletionPercentage());

        Map<String, Boolean> checkpoints = new LinkedHashMap<>();
        checkpoints.put("Basic Information",
                isValid(candidate.getFirstName()) && isValid(candidate.getLastName()) && isValid(candidate.getEmail()));
        checkpoints.put("Profile Picture", isValid(candidate.getProfilePictureUrl()));
        checkpoints.put("Professional Headline", isValid(candidate.getHeadline()));
        checkpoints.put("Summary (50+ chars)",
                isValid(candidate.getSummary()) && candidate.getSummary().length() >= 50);
        checkpoints.put("Work Experience",
                candidate.getExperienceJson() != null && !candidate.getExperienceJson().equals("[]"));
        checkpoints.put("Education",
                candidate.getEducationJson() != null && !candidate.getEducationJson().equals("[]"));
        checkpoints.put("Skills (3+)", candidate.getSkillsList().size() >= 3);
        // CV is optional - removed from checkpoints (but still shown as optional improvement)
        checkpoints.put("Resume Uploaded", candidate.isResumeUploaded());
        checkpoints.put("Job Preferences",
                (candidate.getPreferredJobTypesList() != null && !candidate.getPreferredJobTypesList().isEmpty()) &&
                        (candidate.getPreferredLocationsList() != null && !candidate.getPreferredLocationsList().isEmpty()));

        health.setCheckpoints(checkpoints);

        List<String> suggestions = new ArrayList<>();
        if (!checkpoints.get("Profile Picture")) suggestions.add("Add a profile picture");
        if (!checkpoints.get("Professional Headline")) suggestions.add("Add a professional headline");
        if (!checkpoints.get("Summary (50+ chars)")) suggestions.add("Write a compelling summary (minimum 50 characters)");
        if (!checkpoints.get("Work Experience")) suggestions.add("Add your work experience");
        if (!checkpoints.get("Skills (3+)")) suggestions.add("Add at least 3 skills");
        if (!checkpoints.get("Resume Uploaded")) suggestions.add("Upload your resume for better matching");
        if (!checkpoints.get("Job Preferences")) suggestions.add("Set your job preferences");

        health.setSuggestions(suggestions);

        return health;
    }

    private List<RecommendedJob> getRecommendedJobs(CandidateProfile candidate) {
        if (!candidate.canApplyForJobs()) {
            return new ArrayList<>();
        }

        List<Job> activeJobs = jobRepository.findByIsActiveTrue(PageRequest.of(0, 50));

        // Don't filter out applied jobs - just show them with applied flag
        return activeJobs.stream()
                .filter(job -> job.isActive() && job.isApplicationOpen())
                // REMOVE THIS LINE ↓
                // .filter(job -> !job.hasApplied(candidate))
                .map(job -> {
                    RecommendedJob recommended = new RecommendedJob();
                    recommended.setId(job.getId());
                    recommended.setTitle(job.getTitle());
                    recommended.setCompanyName(job.getCompany().getCompanyName());
                    recommended.setLocation(job.getLocation());
                    recommended.setJobType(job.getJobType().name());
                    recommended.setMatchScore(matchScoreService.calculateMatchScore(candidate, job));
                    recommended.setIsRemote(job.isRemote());
                    recommended.setSalaryRange(formatSalaryRange(job));
                    recommended.setPostedDate(job.getCreatedAt());
                    recommended.setHasApplied(job.hasApplied(candidate)); // Add this flag
                    return recommended;
                })
                .sorted((a, b) -> b.getMatchScore() - a.getMatchScore())
                .limit(5)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public JobApplicationResponse updateApplication(Long userId, Long applicationId,
                                                    JobApplicationRequest request) {
        JobApplication application = jobApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new NotFoundException("Application not found"));

        if (!application.getCandidate().getUser().getId().equals(userId)) {
            throw new UnauthorizedAccessException("You can only update your own applications");
        }

        if (!canUpdateApplication(application)) {
            throw new BadRequestException("Cannot update application in current status");
        }

        // Update application fields - only cover letter can be updated
        if (request.getCoverLetter() != null) {
            application.setCoverLetter(request.getCoverLetter());
        }

        JobApplication saved = jobApplicationRepository.save(application);
        return mapToApplicationResponse(saved);
    }

    private boolean canUpdateApplication(JobApplication application) {
        return !application.isWithdrawn() &&
                (application.getStatus() == ApplicationStatus.APPLIED ||
                        application.getStatus() == ApplicationStatus.VIEWED);
    }

    @Override
    @Transactional
    public JobApplicationResponse toggleFavorite(Long userId, Long applicationId) {
        JobApplication application = jobApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new NotFoundException("Application not found"));

        if (!application.getCandidate().getUser().getId().equals(userId)) {
            throw new UnauthorizedAccessException("You can only modify your own applications");
        }

        application.setFavorite(!application.isFavorite());
        JobApplication saved = jobApplicationRepository.save(application);

        String action = saved.isFavorite() ? "added to" : "removed from";
        log.info("Application {} {} favorites by user {}", applicationId, action, userId);

        return mapToApplicationResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasAppliedForJob(Long userId, Long jobId) {
        return jobApplicationRepository.findByUserIdAndJobId(userId, jobId).isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobApplicationResponse> getApplicationsByStatus(Long userId, ApplicationStatus status) {
        List<JobApplication> applications = jobApplicationRepository.findByUserIdAndStatus(userId, status);
        return applications.stream()
                .map(this::mapToApplicationResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public JobApplicationResponse reapplyForJob(Long userId, Long applicationId, JobApplicationRequest request) {
        JobApplication application = jobApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new NotFoundException("Application not found"));

        // Check ownership
        if (!application.getCandidate().getUser().getId().equals(userId)) {
            throw new UnauthorizedAccessException("You can only reapply to your own applications");
        }

        // Check if application is withdrawn
        if (!application.isWithdrawn()) {
            throw new BadRequestException("You can only reapply to withdrawn applications");
        }

        // Validate candidate eligibility
        validateCandidateEligibility(application.getCandidate());

        Job job = application.getJob();

        // Validate job eligibility
        validateJobEligibility(job, application.getCandidate());

        // Check if job is still open for applications
        if (!job.isActive() || !job.isApplicationOpen()) {
            throw new BadRequestException("This job is no longer accepting applications");
        }

        // Check if there's already an active application for this job
        if (jobApplicationRepository.existsByJobIdAndCandidateIdAndIsWithdrawnFalse(
                job.getId(), application.getCandidate().getId())) {
            throw new BadRequestException("You already have an active application for this job");
        }

        // Update application for reapplication
        application.setWithdrawn(false);
        application.setStatus(ApplicationStatus.APPLIED);
        application.setStatusChangedAt(LocalDateTime.now());
        application.setAppliedAt(LocalDateTime.now());

        // Update cover letter if provided
        if (request.getCoverLetter() != null) {
            application.setCoverLetter(request.getCoverLetter());
        }

        // Recalculate match score
        int matchScore = matchScoreService.calculateMatchScore(application.getCandidate(), job);
        application.setMatchScore(matchScore);
        application.setMatchNotes(matchScoreService.generateMatchNotes(
                application.getCandidate(), job, matchScore));

        JobApplication saved = jobApplicationRepository.save(application);

        // Update job application count
        updateJobApplicationCount(job);

        log.info("Candidate {} reapplied for job {} (applicationId: {})",
                userId, job.getId(), applicationId);

        return mapToApplicationResponse(saved);
    }

    // =========== MAPPING METHODS ===========

    private CandidateProfileResponse mapToProfileResponse(CandidateProfile profile) {
        CandidateProfileResponse response = new CandidateProfileResponse();

        response.setId(profile.getId());
        response.setUserId(profile.getUser().getId());
        response.setFirstName(profile.getFirstName());
        response.setLastName(profile.getLastName());
        response.setFullName(profile.getFullName());
        response.setEmail(profile.getEmail());
        response.setPhone(profile.getPhone());
        response.setDateOfBirth(profile.getDateOfBirth());
        response.setGender(profile.getGender());
        response.setAddress(profile.getAddress());
        response.setCity(profile.getCity());
        response.setCountry(profile.getCountry());
        response.setProfilePictureUrl(profile.getProfilePictureUrl());

        response.setHeadline(profile.getHeadline());
        response.setSummary(profile.getSummary());
        response.setTotalExperienceYears(profile.getTotalExperienceYears());
        response.setCurrentSalary(profile.getCurrentSalary());
        response.setExpectedSalary(profile.getExpectedSalary());
        response.setSalaryCurrency(profile.getSalaryCurrency());
        response.setNoticePeriod(profile.getNoticePeriod());

        try {
            if (profile.getEducationJson() != null && !profile.getEducationJson().isEmpty()) {
                List<EducationRequest> eduList = objectMapper.readValue(profile.getEducationJson(),
                        new TypeReference<List<EducationRequest>>() {});
                List<EducationResponse> educationResponses = eduList.stream()
                        .map(this::mapToEducationResponse)
                        .collect(Collectors.toList());
                response.setEducation(educationResponses);
            }
            if (profile.getExperienceJson() != null && !profile.getExperienceJson().isEmpty()) {
                List<ExperienceRequest> expList = objectMapper.readValue(profile.getExperienceJson(),
                        new TypeReference<List<ExperienceRequest>>() {});
                List<ExperienceResponse> experienceResponses = expList.stream()
                        .map(this::mapToExperienceResponse)
                        .collect(Collectors.toList());
                response.setExperience(experienceResponses);
            }
            if (profile.getCertificationsJson() != null && !profile.getCertificationsJson().isEmpty()) {
                List<CertificationRequest> certList = objectMapper.readValue(profile.getCertificationsJson(),
                        new TypeReference<List<CertificationRequest>>() {});
                List<CertificationResponse> certResponses = certList.stream()
                        .map(this::mapToCertificationResponse)
                        .collect(Collectors.toList());
                response.setCertifications(certResponses);
            }
            if (profile.getLanguagesJson() != null && !profile.getLanguagesJson().isEmpty()) {
                List<LanguageRequest> langList = objectMapper.readValue(profile.getLanguagesJson(),
                        new TypeReference<List<LanguageRequest>>() {});
                List<LanguageResponse> langResponses = langList.stream()
                        .map(this::mapToLanguageResponse)
                        .collect(Collectors.toList());
                response.setLanguages(langResponses);
            }
        } catch (JsonProcessingException e) {
            log.error("Error parsing JSON from profile", e);
        }

        response.setSkills(profile.getSkillsList());

        response.setPreferredJobTypes(profile.getPreferredJobTypesList());
        response.setPreferredLocations(profile.getPreferredLocationsList());

        try {
            if (profile.getPreferredIndustriesJson() != null && !profile.getPreferredIndustriesJson().isEmpty()) {
                List<String> industries = objectMapper.readValue(profile.getPreferredIndustriesJson(),
                        new TypeReference<List<String>>() {});
                response.setPreferredIndustries(industries);
            }
        } catch (JsonProcessingException e) {
            log.error("Error parsing industries JSON", e);
        }

        response.setRemotePreference(profile.getRemotePreference());
        response.setResumeUrl(profile.getResumeUrl());
        response.setResumeFileName(profile.getResumeFileName());
        response.setIsResumeUploaded(profile.isResumeUploaded());
        response.setProfileVisibility(profile.getProfileVisibility());
        response.setIsProfileComplete(profile.isProfileComplete());
        response.setCompletionPercentage(profile.getCompletionPercentage());
        response.setIsFeatured(profile.isFeatured());
        response.setIsActivelyLooking(profile.isActivelyLooking());
        response.setAvailableFrom(profile.getAvailableFrom());
        response.setLastActiveAt(profile.getLastActiveAt());
        response.setCreatedAt(profile.getCreatedAt());
        response.setUpdatedAt(profile.getUpdatedAt());

        Integer totalApps = jobApplicationRepository.countByCandidateId(profile.getId());
        response.setTotalApplications(totalApps != null ? totalApps : 0);

        Map<String, Integer> statusCount = new HashMap<>();
        for (ApplicationStatus status : ApplicationStatus.values()) {
            Integer count = jobApplicationRepository.countByCandidateIdAndStatus(profile.getId(), status);
            if (count != null && count > 0) {
                statusCount.put(status.name(), count);
            }
        }
        response.setApplicationStatusCount(statusCount);

        // Updated: CV is optional, actively looking is optional
        boolean canApply = profile.getUser().isActive() &&
                profile.getUser().isEmailVerified() &&
                profile.isProfileComplete();
        response.setCanApplyForJobs(canApply);

        return response;
    }

    private boolean canReapplyApplication(JobApplication application) {
        return application.isWithdrawn() &&
                application.getJob().isActive() &&
                application.getJob().isApplicationOpen() &&
                !jobApplicationRepository.existsByJobIdAndCandidateIdAndIsWithdrawnFalse(
                        application.getJob().getId(), application.getCandidate().getId());
    }

    private JobApplicationResponse mapToApplicationResponse(JobApplication application) {
        JobApplicationResponse response = new JobApplicationResponse();

        response.setId(application.getId());
        response.setJobId(application.getJob().getId());
        response.setJobTitle(application.getJob().getTitle());
        response.setCompanyId(application.getJob().getCompany().getId());
        response.setCompanyName(application.getJob().getCompany().getCompanyName());
        response.setCompanyLogo(application.getJob().getCompany().getLogoUrl());
        response.setStatus(application.getStatus());
        response.setAppliedAt(application.getAppliedAt());
        response.setStatusChangedAt(application.getStatusChangedAt());
        response.setCoverLetter(application.getCoverLetter());

        // No answers field

        response.setMatchScore(application.getMatchScore());
        response.setMatchNotes(application.getMatchNotes());
        response.setIsWithdrawn(application.isWithdrawn());
        response.setIsFavorite(application.isFavorite());
        response.setCreatedAt(application.getCreatedAt());
        response.setUpdatedAt(application.getUpdatedAt());

        // Job details
        Job job = application.getJob();
        response.setLocation(job.getLocation());
        response.setJobType(job.getJobType().name());
        response.setIsRemote(job.isRemote());
        response.setSalaryRange(formatSalaryRange(job));
        response.setExperienceLevel(job.getExperienceLevel().name());
        response.setApplicationDeadline(job.getApplicationDeadline());
        response.setIsApplicationOpen(job.isApplicationOpen());

        // Permissions
        response.setCanWithdraw(canWithdrawApplication(application));
        response.setCanUpdate(canUpdateApplication(application));
        response.setCanReapply(canReapplyApplication(application));

        return response;
    }

    private String formatSalaryRange(Job job) {
        if (job.getSalaryMin() == null && job.getSalaryMax() == null) {
            return "Negotiable";
        }
        if (job.getSalaryMin() != null && job.getSalaryMax() != null) {
            return job.getSalaryMin() + " - " + job.getSalaryMax() + " " + job.getSalaryCurrency();
        }
        if (job.getSalaryMin() != null) {
            return "From " + job.getSalaryMin() + " " + job.getSalaryCurrency();
        }
        return "Up to " + job.getSalaryMax() + " " + job.getSalaryCurrency();
    }

    private EducationResponse mapToEducationResponse(EducationRequest edu) {
        EducationResponse response = new EducationResponse();
        response.setInstitution(edu.getInstitution());
        response.setDegree(edu.getDegree());
        response.setFieldOfStudy(edu.getFieldOfStudy());
        response.setGrade(edu.getGrade());
        response.setDescription(edu.getDescription());
        response.setStartDate(edu.getStartDate());
        response.setEndDate(edu.getEndDate());
        response.setIsCurrent(edu.getIsCurrent());
        response.setLocation(edu.getLocation());

        if (edu.getStartDate() != null) {
            if (edu.getIsCurrent() != null && edu.getIsCurrent()) {
                response.setDuration(edu.getStartDate().getYear() + " - Present");
            } else if (edu.getEndDate() != null) {
                response.setDuration(edu.getStartDate().getYear() + " - " + edu.getEndDate().getYear());
            }
        }

        return response;
    }

    private ExperienceResponse mapToExperienceResponse(ExperienceRequest exp) {
        ExperienceResponse response = new ExperienceResponse();
        response.setCompany(exp.getCompany());
        response.setPosition(exp.getPosition());
        response.setEmploymentType(exp.getEmploymentType());
        response.setLocation(exp.getLocation());
        response.setIsCurrent(exp.getIsCurrent());
        response.setStartDate(exp.getStartDate());
        response.setEndDate(exp.getEndDate());
        response.setDescription(exp.getDescription());
        response.setAchievements(exp.getAchievements());
        response.setSkillsUsed(exp.getSkillsUsed());

        if (exp.getStartDate() != null) {
            if (exp.getIsCurrent() != null && exp.getIsCurrent()) {
                response.setExperienceInMonths((int) ChronoUnit.MONTHS.between(exp.getStartDate(), LocalDate.now()));
            } else if (exp.getEndDate() != null) {
                response.setExperienceInMonths((int) ChronoUnit.MONTHS.between(exp.getStartDate(), exp.getEndDate()));
            }
        }

        return response;
    }

    private CertificationResponse mapToCertificationResponse(CertificationRequest cert) {
        CertificationResponse response = new CertificationResponse();
        response.setName(cert.getName());
        response.setIssuingOrganization(cert.getIssuingOrganization());
        response.setCredentialId(cert.getCredentialId());
        response.setCredentialUrl(cert.getCredentialUrl());
        response.setIssueDate(cert.getIssueDate());
        response.setExpirationDate(cert.getExpirationDate());
        response.setDoesNotExpire(cert.getDoesNotExpire());
        return response;
    }

    private LanguageResponse mapToLanguageResponse(LanguageRequest lang) {
        LanguageResponse response = new LanguageResponse();
        response.setLanguage(lang.getLanguage());
        response.setProficiency(lang.getProficiency());
        return response;
    }

    private RecentApplication mapToRecentApplication(JobApplication application) {
        RecentApplication recent = new RecentApplication();

        recent.setId(application.getId());
        recent.setJobTitle(application.getJob().getTitle());
        recent.setCompanyName(application.getJob().getCompany().getCompanyName());
        recent.setStatus(application.getStatus().name());
        recent.setAppliedDate(application.getAppliedAt().toLocalDate().toString());
        recent.setMatchScore(application.getMatchScore());

        return recent;
    }

    private boolean isValid(String value) {
        return value != null && !value.trim().isEmpty();
    }
}