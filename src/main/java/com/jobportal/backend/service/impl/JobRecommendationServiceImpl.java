package com.jobportal.backend.service.impl;

import com.jobportal.backend.dto.RecommendationScore;
import com.jobportal.backend.dto.job.response.JobRecommendationResponse;
import com.jobportal.backend.entity.*;
import com.jobportal.backend.exception.BadRequestException;
import com.jobportal.backend.exception.NotFoundException;
import com.jobportal.backend.repository.*;
import com.jobportal.backend.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobRecommendationServiceImpl implements JobRecommendationService {

    private final CandidateProfileRepository candidateProfileRepository;
    private final JobRepository jobRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final MatchScoreService matchScoreService;  // ← ADD THIS
    private final SkillMatchService skillMatchService;
    private final ExperienceMatchService experienceMatchService;
    private final LocationMatchService locationMatchService;
    private final JobTypeMatchService jobTypeMatchService;

    // Weight configuration for recommendation algorithm
    private static final double SKILL_WEIGHT = 0.40;      // 40% (increased to match MatchScoreService)
    private static final double EXPERIENCE_WEIGHT = 0.30; // 30%
    private static final double LOCATION_WEIGHT = 0.20;   // 20%
    private static final double JOB_TYPE_WEIGHT = 0.10;   // 10%

    // Trending job calculation window (days)
    private static final int TRENDING_WINDOW_DAYS = 7;

    @Override
    @Transactional(readOnly = true)
    public Page<JobRecommendationResponse> getPersonalizedRecommendations(
            Long candidateId, int limit, int minScore,
            String jobType, String location, Pageable pageable) {

        log.info("Getting personalized recommendations for candidate: {}", candidateId);

        CandidateProfile candidate = candidateProfileRepository.findByUserId(candidateId)
                .orElseThrow(() -> new NotFoundException("Candidate profile not found"));

        // Validate candidate eligibility
        if (!isEligibleForRecommendations(candidateId)) {
            throw new BadRequestException("Complete your profile to get personalized recommendations");
        }

        // Get active jobs with pagination
        Page<Job> activeJobs = jobRepository.findByIsActiveAndCompanyIsActiveAndCompanyIsVerified(
                true, true, true, pageable);

        // Calculate scores and create recommendations
        List<JobRecommendationResponse> recommendations = activeJobs.getContent().stream()
                .filter(job -> !hasAppliedForJob(candidate.getId(), job.getId()))
                .filter(Job::isApplicationOpen)
                .filter(job -> filterByTypeAndLocation(job, jobType, location))
                .map(job -> {
                    // USE MATCH SCORE SERVICE FOR CONSISTENCY
                    int matchScore = matchScoreService.calculateMatchScore(candidate, job);
                    RecommendationScore score = createRecommendationScoreFromMatch(matchScore, candidate, job);
                    return createRecommendationResponse(job, candidate, score, matchScore);
                })
                .filter(response -> response.getMatchScore() >= minScore)
                .sorted((a, b) -> Double.compare(b.getRecommendationScore(), a.getRecommendationScore()))
                .limit(Math.min(limit, 100))
                .collect(Collectors.toList());

        log.info("Found {} recommendations for candidate {}", recommendations.size(), candidateId);

        return new PageImpl<>(recommendations, pageable, recommendations.size());
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobRecommendationResponse> getTrendingJobs(Long candidateId, int limit) {
        log.info("Getting trending jobs for candidate: {}", candidateId);

        CandidateProfile candidate = candidateProfileRepository.findByUserId(candidateId)
                .orElseThrow(() -> new NotFoundException("Candidate profile not found"));

        LocalDateTime startDate = LocalDateTime.now().minusDays(TRENDING_WINDOW_DAYS);

        List<Object[]> trendingData = jobRepository.findTrendingJobsData(startDate);

        List<JobRecommendationResponse> trendingJobs = trendingData.stream()
                .map(data -> {
                    Long jobId = (Long) data[0];
                    Integer applicationCount = ((Number) data[1]).intValue();

                    return jobRepository.findById(jobId)
                            .filter(job -> job.isActive() && job.isApplicationOpen())
                            .filter(job -> !hasAppliedForJob(candidate.getId(), job.getId()))
                            .map(job -> {
                                int matchScore = matchScoreService.calculateMatchScore(candidate, job);
                                RecommendationScore score = createRecommendationScoreFromMatch(matchScore, candidate, job);
                                JobRecommendationResponse response = createRecommendationResponse(job, candidate, score, matchScore);
                                response.setTrendingRank(applicationCount);
                                response.setRecommendationReasons(Arrays.asList(
                                        "Trending: " + applicationCount + " applications in last " + TRENDING_WINDOW_DAYS + " days",
                                        "Popular in " + job.getCompany().getIndustry().getName() + " industry"
                                ));
                                return response;
                            })
                            .orElse(null);
                })
                .filter(Objects::nonNull)
                .sorted((a, b) -> Integer.compare(b.getTrendingRank(), a.getTrendingRank()))
                .limit(limit)
                .collect(Collectors.toList());

        if (trendingJobs.size() < limit) {
            int remaining = limit - trendingJobs.size();
            List<JobRecommendationResponse> personalized = getPersonalizedRecommendations(
                    candidateId, remaining, 30, null, null, Pageable.ofSize(remaining)
            ).getContent();
            trendingJobs.addAll(personalized);
        }

        log.info("Found {} trending jobs for candidate {}", trendingJobs.size(), candidateId);
        return trendingJobs;
    }

    @Override
    public RecommendationScore calculateRecommendationScore(Long candidateId, Long jobId) {
        CandidateProfile candidate = candidateProfileRepository.findByUserId(candidateId)
                .orElseThrow(() -> new NotFoundException("Candidate profile not found"));

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new NotFoundException("Job not found"));

        int matchScore = matchScoreService.calculateMatchScore(candidate, job);
        return createRecommendationScoreFromMatch(matchScore, candidate, job);
    }

    @Override
    public List<String> getRecommendationReasons(Long candidateId, Long jobId) {
        CandidateProfile candidate = candidateProfileRepository.findByUserId(candidateId)
                .orElseThrow(() -> new NotFoundException("Candidate profile not found"));

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new NotFoundException("Job not found"));

        return generateRecommendationReasons(candidate, job);
    }

    // =========== NEW HELPER METHOD ===========

    private RecommendationScore createRecommendationScoreFromMatch(int matchScore, CandidateProfile candidate, Job job) {
        RecommendationScore score = new RecommendationScore();

        try {
            // Distribute the total match score back to individual components
            // This maintains backward compatibility while ensuring consistency

            double skillPercentage = calculateSkillPercentage(candidate, job);
            double experiencePercentage = calculateExperiencePercentage(candidate, job);
            double locationPercentage = calculateLocationPercentage(candidate, job);
            double jobTypePercentage = calculateJobTypePercentage(candidate, job);

            // Calculate individual scores based on matchScore and percentages
            score.setSkillScore((matchScore * SKILL_WEIGHT) * skillPercentage);
            score.setExperienceScore((matchScore * EXPERIENCE_WEIGHT) * experiencePercentage);
            score.setLocationScore((matchScore * LOCATION_WEIGHT) * locationPercentage);
            score.setJobTypeScore((matchScore * JOB_TYPE_WEIGHT) * jobTypePercentage);
            score.setSalaryScore(0.0);
            score.setTrendingScore(0.0);
            score.setRecencyScore(0.0);
            score.setTotalScore(matchScore);

        } catch (Exception e) {
            log.error("Error creating recommendation score", e);
            score.setTotalScore(matchScore);
            score.setSkillScore(matchScore * SKILL_WEIGHT);
            score.setExperienceScore(matchScore * EXPERIENCE_WEIGHT);
            score.setLocationScore(matchScore * LOCATION_WEIGHT);
            score.setJobTypeScore(matchScore * JOB_TYPE_WEIGHT);
        }

        return score;
    }

    private double calculateSkillPercentage(CandidateProfile candidate, Job job) {
        List<String> candidateSkills = candidate.getSkillsList();
        List<String> jobSkills = job.getRequiredSkillsList();

        if (candidateSkills.isEmpty() || jobSkills.isEmpty()) {
            return 0.5; // Default 50%
        }

        long matchingSkills = candidateSkills.stream()
                .map(String::toLowerCase)
                .filter(skill -> jobSkills.stream()
                        .anyMatch(jobSkill -> jobSkill.toLowerCase().contains(skill) || skill.contains(jobSkill.toLowerCase())))
                .count();

        return (double) matchingSkills / jobSkills.size();
    }

    private double calculateExperiencePercentage(CandidateProfile candidate, Job job) {
        Integer candidateExp = candidate.getTotalExperienceYears();
        if (candidateExp == null) return 0.5;

        switch (job.getExperienceLevel()) {
            case INTERN:
                return candidateExp <= 1 ? 1.0 : Math.max(0, 1.0 - (candidateExp - 1) * 0.2);
            case JUNIOR_LEVEL:
                return candidateExp <= 3 ? 1.0 : Math.max(0, 1.0 - (candidateExp - 3) * 0.15);
            case MID_LEVEL:
                if (candidateExp >= 2 && candidateExp <= 5) return 1.0;
                if (candidateExp < 2) return 0.5;
                return Math.max(0, 1.0 - (candidateExp - 5) * 0.1);
            case SENIOR_LEVEL:
                if (candidateExp >= 5 && candidateExp <= 10) return 1.0;
                if (candidateExp < 5) return 0.5 + (candidateExp / 10.0);
                return Math.max(0, 1.0 - (candidateExp - 10) * 0.05);
            case EXECUTIVE:
                return candidateExp >= 10 ? 1.0 : candidateExp / 10.0;
            default:
                return 0.5;
        }
    }

    private double calculateLocationPercentage(CandidateProfile candidate, Job job) {
        if (job.isRemote()) {
            if (candidate.getRemotePreference() != null && candidate.getRemotePreference()) {
                return 1.0;
            }
            return 0.8;
        }

        List<String> preferredLocations = candidate.getPreferredLocationsList();
        if (preferredLocations.isEmpty()) return 0.5;

        boolean locationMatch = preferredLocations.stream()
                .anyMatch(loc -> job.getLocation().toLowerCase().contains(loc.toLowerCase()));

        return locationMatch ? 1.0 : 0.3;
    }

    private double calculateJobTypePercentage(CandidateProfile candidate, Job job) {
        List<String> preferredJobTypes = candidate.getPreferredJobTypesList();
        if (preferredJobTypes.isEmpty()) return 0.5;

        boolean jobTypeMatch = preferredJobTypes.stream()
                .anyMatch(type -> type.equalsIgnoreCase(job.getJobType().name()));

        return jobTypeMatch ? 1.0 : 0.2;
    }

    private JobRecommendationResponse createRecommendationResponse(
            Job job, CandidateProfile candidate, RecommendationScore score, int matchScore) {

        JobRecommendationResponse response = new JobRecommendationResponse();

        // Basic job info
        response.setId(job.getId());
        response.setTitle(job.getTitle());
        response.setCompanyId(job.getCompany().getId());
        response.setCompanyName(job.getCompany().getCompanyName());
        response.setCompanyLogo(job.getCompany().getLogoUrl());
        response.setLocation(job.getLocation());
        response.setJobType(job.getJobType().name());
        response.setIsRemote(job.isRemote());
        response.setExperienceLevel(job.getExperienceLevel().name());
        response.setCreatedAt(job.getCreatedAt());
        response.setApplicationDeadline(job.getApplicationDeadline());
        response.setIsApplicationOpen(job.isApplicationOpen());

        // Salary info
        response.setSalaryMin(job.getSalaryMin());
        response.setSalaryMax(job.getSalaryMax());
        response.setSalaryCurrency(job.getSalaryCurrency());

        // Scores - USE THE MATCH SCORE
        response.setRecommendationScore((double) matchScore);
        response.setMatchScore(matchScore);

        // Score breakdown
        Map<String, Double> scoreBreakdown = new LinkedHashMap<>();
        scoreBreakdown.put("Skills", score.getSkillScore());
        scoreBreakdown.put("Experience", score.getExperienceScore());
        scoreBreakdown.put("Location", score.getLocationScore());
        scoreBreakdown.put("Job Type", score.getJobTypeScore());
        response.setScoreBreakdown(scoreBreakdown);

        // Recommendation reasons
        response.setRecommendationReasons(generateRecommendationReasons(candidate, job));

        return response;
    }

    private List<String> generateRecommendationReasons(CandidateProfile candidate, Job job) {
        List<String> reasons = new ArrayList<>();

        // Skill-based reasons
        List<String> matchingSkills = skillMatchService.findMatchingSkills(
                candidate.getSkillsList(), job.getRequiredSkillsList());
        if (!matchingSkills.isEmpty()) {
            if (matchingSkills.size() >= 5) {
                reasons.add("Excellent skill match: " + matchingSkills.size() + " skills match");
            } else if (matchingSkills.size() >= 3) {
                reasons.add("Good skill match: " + matchingSkills.size() + " key skills match");
            } else {
                reasons.add("Matches your skills in: " + String.join(", ", matchingSkills.subList(0, Math.min(3, matchingSkills.size()))));
            }
        } else if (!job.getRequiredSkillsList().isEmpty()) {
            reasons.add("Consider adding these skills: " + String.join(", ", job.getRequiredSkillsList().stream().limit(3).collect(Collectors.toList())));
        }

        // Experience reason
        if (candidate.getTotalExperienceYears() != null) {
            String expLevel = job.getExperienceLevel().name().toLowerCase().replace("_", " ");
            reasons.add("Your " + candidate.getTotalExperienceYears() + " years of experience (" + expLevel + " level)");
        }

        // Location reason
        List<String> preferredLocations = candidate.getPreferredLocationsList();
        if (job.isRemote()) {
            if (candidate.getRemotePreference() != null && candidate.getRemotePreference()) {
                reasons.add("Perfect remote opportunity matching your preference");
            } else {
                reasons.add("Remote position available");
            }
        } else if (preferredLocations != null && !preferredLocations.isEmpty()) {
            if (preferredLocations.stream().anyMatch(loc ->
                    job.getLocation().toLowerCase().contains(loc.toLowerCase()))) {
                reasons.add("Located in your preferred area: " + job.getLocation());
            }
        }

        // Job type reason
        List<String> preferredJobTypes = candidate.getPreferredJobTypesList();
        if (preferredJobTypes != null && preferredJobTypes.contains(job.getJobType().name())) {
            reasons.add("Matches your preferred job type: " + job.getJobType().name());
        }

        // Salary reason
        if (candidate.getExpectedSalary() != null &&
                job.getSalaryMin() != null && job.getSalaryMax() != null) {
            if (candidate.getExpectedSalary().compareTo(job.getSalaryMax()) <= 0) {
                reasons.add("Salary range matches your expectations");
            } else if (candidate.getExpectedSalary().compareTo(job.getSalaryMin()) <= 0) {
                reasons.add("Salary is within your expected range");
            }
        }

        // If no specific reasons, add generic ones
        if (reasons.isEmpty()) {
            reasons.add("Based on your profile and skills");
            reasons.add("Opportunity in " + job.getCompany().getIndustry().getName() + " industry");
        }

        return reasons;
    }

    private boolean hasAppliedForJob(Long candidateId, Long jobId) {
        return jobApplicationRepository.existsByCandidateIdAndJobId(candidateId, jobId);
    }

    private boolean filterByTypeAndLocation(Job job, String jobType, String location) {
        if (jobType != null && !jobType.trim().isEmpty()) {
            if (!job.getJobType().name().equalsIgnoreCase(jobType.trim())) {
                return false;
            }
        }

        if (location != null && !location.trim().isEmpty()) {
            if (!job.getLocation().toLowerCase().contains(location.toLowerCase().trim())) {
                return false;
            }
        }

        return true;
    }

    private boolean isEligibleForRecommendations(Long candidateId) {
        CandidateProfile candidate = candidateProfileRepository.findByUserId(candidateId)
                .orElseThrow(() -> new NotFoundException("Candidate profile not found"));

        return candidate.getCompletionPercentage() >= 60 &&
                candidate.getUser().isActive() &&
                candidate.getUser().isEmailVerified();
    }
}