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
    private final SkillMatchService skillMatchService;
    private final ExperienceMatchService experienceMatchService;
    private final LocationMatchService locationMatchService;
    private final JobTypeMatchService jobTypeMatchService;

    // Weight configuration for recommendation algorithm
    private static final double SKILL_WEIGHT = 0.25;      // 25%
    private static final double EXPERIENCE_WEIGHT = 0.20; // 20%
    private static final double LOCATION_WEIGHT = 0.15;   // 15%
    private static final double JOB_TYPE_WEIGHT = 0.10;   // 10%
    private static final double SALARY_WEIGHT = 0.10;     // 10%
    private static final double TRENDING_WEIGHT = 0.05;   // 5%
    private static final double RECENCY_WEIGHT = 0.10;    // 10%

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
                    RecommendationScore score = calculateRecommendationScore(candidate, job);
                    return createRecommendationResponse(job, candidate, score);
                })
                .filter(response -> response.getMatchScore() >= minScore)
                .sorted((a, b) -> Double.compare(b.getRecommendationScore(), a.getRecommendationScore()))
                .limit(Math.min(limit, 100)) // Safety limit
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

        // Get trending jobs (most applications in recent days)
        LocalDateTime startDate = LocalDateTime.now().minusDays(TRENDING_WINDOW_DAYS);

        // Get jobs with high application counts in recent days
        List<Object[]> trendingData = jobRepository.findTrendingJobsData(startDate);

        List<JobRecommendationResponse> trendingJobs = trendingData.stream()
                .map(data -> {
                    Long jobId = (Long) data[0];
                    Integer applicationCount = ((Number) data[1]).intValue();

                    return jobRepository.findById(jobId)
                            .filter(job -> job.isActive() && job.isApplicationOpen())
                            .filter(job -> !hasAppliedForJob(candidate.getId(), job.getId()))
                            .map(job -> {
                                RecommendationScore score = calculateRecommendationScore(candidate, job);
                                JobRecommendationResponse response = createRecommendationResponse(job, candidate, score);
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

        // If not enough trending jobs, supplement with personalized recommendations
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

        return calculateRecommendationScore(candidate, job);
    }

    @Override
    public List<String> getRecommendationReasons(Long candidateId, Long jobId) {
        CandidateProfile candidate = candidateProfileRepository.findByUserId(candidateId)
                .orElseThrow(() -> new NotFoundException("Candidate profile not found"));

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new NotFoundException("Job not found"));

        return generateRecommendationReasons(candidate, job);
    }

    // =========== PRIVATE HELPER METHODS ===========

    private RecommendationScore calculateRecommendationScore(CandidateProfile candidate, Job job) {
        RecommendationScore score = new RecommendationScore();

        try {
            // Start with base score (10 points)
            double baseScore = 10.0;

            // 1. Skill Match Score
            List<String> candidateSkills = candidate.getSkillsList();
            List<String> jobSkills = job.getRequiredSkillsList();

            // If candidate has no skills but job requires skills, give partial score
            if ((candidateSkills == null || candidateSkills.isEmpty()) &&
                    (jobSkills != null && !jobSkills.isEmpty())) {
                score.setSkillScore(5.0 * SKILL_WEIGHT); // Partial score
            } else {
                int skillMatchPoints = skillMatchService.calculateSkillMatchScore(candidateSkills, jobSkills);
                score.setSkillScore(skillMatchPoints * SKILL_WEIGHT);
            }

            // 2. Experience Match Score - More generous
            Integer candidateExpYears = candidate.getTotalExperienceYears();
            if (candidateExpYears != null) {
                int experiencePoints = experienceMatchService.calculateExperienceMatchScore(
                        candidateExpYears.floatValue(), job.getExperienceLevel());
                score.setExperienceScore(experiencePoints * EXPERIENCE_WEIGHT);
            } else {
                // If no experience specified, give average score
                score.setExperienceScore(15.0 * EXPERIENCE_WEIGHT);
            }

            // 3. Location Match Score
            int locationPoints = locationMatchService.calculateLocationMatchScore(candidate, job);
            score.setLocationScore(locationPoints * LOCATION_WEIGHT);

            // 4. Job Type Match Score
            int jobTypePoints = jobTypeMatchService.calculateJobTypeMatchScore(candidate, job);
            score.setJobTypeScore(jobTypePoints * JOB_TYPE_WEIGHT);

            // 5. Salary Match Score - Make it optional
            double salaryScore = calculateSalaryMatchScore(candidate, job);
            score.setSalaryScore(salaryScore * SALARY_WEIGHT);

            // 6. Trending Score - Give minimum for new systems
            double trendingScore = Math.max(calculateTrendingScore(job), 2.0);
            score.setTrendingScore(trendingScore * TRENDING_WEIGHT);

            // 7. Recency Score
            double recencyScore = calculateRecencyScore(job);
            score.setRecencyScore(recencyScore * RECENCY_WEIGHT);

            // Calculate total score with base score
            double totalScore = baseScore +
                    score.getSkillScore() + score.getExperienceScore() +
                    score.getLocationScore() + score.getJobTypeScore() +
                    score.getSalaryScore() + score.getTrendingScore() +
                    score.getRecencyScore();

            // Ensure minimum score of 20 for all active jobs
            score.setTotalScore(Math.max(totalScore, 20.0));

        } catch (Exception e) {
            log.error("Error calculating recommendation score for candidate {} and job {}",
                    candidate.getId(), job.getId(), e);
            // Give minimum score instead of 0
            score.setTotalScore(20.0);
        }

        return score;
    }

    private double calculateSalaryMatchScore(CandidateProfile candidate, Job job) {
        BigDecimal candidateExpected = candidate.getExpectedSalary();
        BigDecimal jobMin = job.getSalaryMin();
        BigDecimal jobMax = job.getSalaryMax();

        // If salary info is missing, give average score
        if (candidateExpected == null || jobMin == null || jobMax == null) {
            return 7.5; // Average score instead of 5
        }

        // Rest of the calculation...
        return 7.5; // Default to average
    }

    private double calculateTrendingScore(Job job) {
        // Calculate how "hot" this job is based on recent applications
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);

        // Get application count in last 7 days
        Long recentApplications = jobApplicationRepository.countByJobIdAndAppliedAtAfter(
                job.getId(), weekAgo);

        if (recentApplications == null) recentApplications = 0L;

        // Normalize to 0-8 scale
        if (recentApplications >= 50) return 8.0;
        if (recentApplications >= 30) return 6.5;
        if (recentApplications >= 15) return 5.0;
        if (recentApplications >= 5) return 3.5;
        if (recentApplications >= 1) return 2.0;
        return 1.0;
    }

    private double calculateRecencyScore(Job job) {
        // Newer jobs get higher score
        LocalDateTime jobCreated = job.getCreatedAt();
        if (jobCreated == null) return 3.5;

        long daysOld = java.time.Duration.between(jobCreated, LocalDateTime.now()).toDays();

        if (daysOld <= 1) return 7.0;   // Today or yesterday
        if (daysOld <= 3) return 6.0;   // Last 3 days
        if (daysOld <= 7) return 5.0;   // Last week
        if (daysOld <= 14) return 4.0;  // Last 2 weeks
        if (daysOld <= 30) return 3.0;  // Last month
        return 2.0;                     // Older
    }

    private JobRecommendationResponse createRecommendationResponse(
            Job job, CandidateProfile candidate, RecommendationScore score) {
        log.debug("Calculating recommendation for job {} - Candidate {}: Total Score = {}",
                job.getId(), candidate.getId(), score.getTotalScore());

        if (score.getTotalScore() < 50) {
            log.debug("Score breakdown for job {}: Skills={}, Experience={}, Location={}, JobType={}, Salary={}, Trending={}, Recency={}",
                    job.getId(), score.getSkillScore(), score.getExperienceScore(),
                    score.getLocationScore(), score.getJobTypeScore(),
                    score.getSalaryScore(), score.getTrendingScore(), score.getRecencyScore());
        }

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

        // Scores
        response.setRecommendationScore(score.getTotalScore());
        response.setMatchScore((int) score.getTotalScore()); // For backward compatibility

        // Score breakdown
        Map<String, Double> scoreBreakdown = new LinkedHashMap<>();
        scoreBreakdown.put("Skills", score.getSkillScore());
        scoreBreakdown.put("Experience", score.getExperienceScore());
        scoreBreakdown.put("Location", score.getLocationScore());
        scoreBreakdown.put("Job Type", score.getJobTypeScore());
        scoreBreakdown.put("Salary", score.getSalaryScore());
        scoreBreakdown.put("Trending", score.getTrendingScore());
        scoreBreakdown.put("Recency", score.getRecencyScore());
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
        }

        // Experience reason
        if (candidate.getTotalExperienceYears() != null) {
            String expLevel = job.getExperienceLevel().name().toLowerCase().replace("_", " ");
            reasons.add("Matches your " + candidate.getTotalExperienceYears() + " years of experience (" + expLevel + " level)");
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
                reasons.add("Located in your preferred area");
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
            }
        }

        // Trending reason
        double trendingScore = calculateTrendingScore(job);
        if (trendingScore >= 6.0) {
            reasons.add("Popular job with high recent interest");
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

        // Minimum profile completion for recommendations
        return candidate.getCompletionPercentage() >= 60 &&
                candidate.getUser().isActive() &&
                candidate.getUser().isEmailVerified();
    }
}