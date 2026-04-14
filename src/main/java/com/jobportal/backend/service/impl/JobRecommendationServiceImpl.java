package com.jobportal.backend.service.impl;

import com.jobportal.backend.dto.RecommendationScore;
import com.jobportal.backend.dto.job.response.JobRecommendationResponse;
import com.jobportal.backend.entity.CandidateProfile;
import com.jobportal.backend.entity.Job;
import com.jobportal.backend.exception.BadRequestException;
import com.jobportal.backend.exception.NotFoundException;
import com.jobportal.backend.repository.CandidateProfileRepository;
import com.jobportal.backend.repository.JobApplicationRepository;
import com.jobportal.backend.repository.JobRepository;
import com.jobportal.backend.service.JobRecommendationService;
import com.jobportal.backend.service.SkillMatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
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

    private static final int TRENDING_WINDOW_DAYS = 7;

    // Recommendation model weights/scores
    private static final double BASE_SCORE = 10.0;

    private static final double SKILLS_WEIGHT = 0.25;      // up to 25
    private static final double EXPERIENCE_WEIGHT = 0.20;  // up to 20
    private static final double LOCATION_WEIGHT = 0.15;    // up to 15
    private static final double JOB_TYPE_WEIGHT = 0.10;    // up to 10
    private static final double SALARY_WEIGHT = 0.10;      // up to 10
    private static final double TRENDING_WEIGHT = 0.05;    // up to small contribution
    private static final double RECENCY_WEIGHT = 0.10;     // up to small contribution

    @Override
    @Transactional(readOnly = true)
    public Page<JobRecommendationResponse> getPersonalizedRecommendations(
            Long candidateId, int limit, int minScore,
            String jobType, String location, Pageable pageable) {

        log.info("Getting personalized recommendations for candidate: {}", candidateId);

        CandidateProfile candidate = candidateProfileRepository.findByUserId(candidateId)
                .orElseThrow(() -> new NotFoundException("Candidate profile not found"));

        if (!isEligibleForRecommendations(candidateId)) {
            throw new BadRequestException("Complete your profile to get personalized recommendations");
        }

        Page<Job> activeJobs = jobRepository.findByIsActiveAndCompanyIsActiveAndCompanyIsVerified(
                true, true, true, pageable
        );

        List<JobRecommendationResponse> recommendations = activeJobs.getContent().stream()
                .filter(job -> !hasAppliedForJob(candidate.getId(), job.getId()))
                .filter(Job::isApplicationOpen)
                .filter(job -> filterByTypeAndLocation(job, jobType, location))
                .map(job -> {
                    RecommendationScore score = calculateRecommendationScoreInternal(candidate, job);
                    return createRecommendationResponse(job, candidate, score);
                })
                .filter(response -> response.getMatchScore() >= minScore)
                .sorted((a, b) -> Double.compare(b.getRecommendationScore(), a.getRecommendationScore()))
                .limit(Math.min(limit, 100))
                .collect(Collectors.toList());

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

                    return jobRepository.findById(jobId)
                            .filter(job -> job.isActive() && job.isApplicationOpen())
                            .filter(job -> !hasAppliedForJob(candidate.getId(), job.getId()))
                            .map(job -> {
                                RecommendationScore score = calculateRecommendationScoreInternal(candidate, job);
                                JobRecommendationResponse response = createRecommendationResponse(job, candidate, score);

                                Integer applicationCount = ((Number) data[1]).intValue();
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

        return trendingJobs;
    }

    @Override
    public RecommendationScore calculateRecommendationScore(Long candidateId, Long jobId) {
        CandidateProfile candidate = candidateProfileRepository.findByUserId(candidateId)
                .orElseThrow(() -> new NotFoundException("Candidate profile not found"));

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new NotFoundException("Job not found"));

        return calculateRecommendationScoreInternal(candidate, job);
    }

    @Override
    public List<String> getRecommendationReasons(Long candidateId, Long jobId) {
        CandidateProfile candidate = candidateProfileRepository.findByUserId(candidateId)
                .orElseThrow(() -> new NotFoundException("Candidate profile not found"));

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new NotFoundException("Job not found"));

        return generateRecommendationReasons(candidate, job);
    }

    private RecommendationScore calculateRecommendationScoreInternal(CandidateProfile candidate, Job job) {
        RecommendationScore score = new RecommendationScore();

        double skillRaw = calculateSkillRawScore(candidate, job);           // 0-100
        double experienceRaw = calculateExperienceRawScore(candidate, job); // 0-100
        double locationRaw = calculateLocationRawScore(candidate, job);     // 0-100
        double jobTypeRaw = calculateJobTypeRawScore(candidate, job);       // 0-100
        double salaryRaw = calculateSalaryRawScore(candidate, job);         // 0-100
        double trendingRaw = calculateTrendingRawScore(job);                // 0-100
        double recencyRaw = calculateRecencyRawScore(job);                  // 0-100

        double skillScore = skillRaw * SKILLS_WEIGHT;
        double experienceScore = experienceRaw * EXPERIENCE_WEIGHT;
        double locationScore = locationRaw * LOCATION_WEIGHT;
        double jobTypeScore = jobTypeRaw * JOB_TYPE_WEIGHT;
        double salaryScore = salaryRaw * SALARY_WEIGHT;
        double trendingScore = trendingRaw * TRENDING_WEIGHT;
        double recencyScore = recencyRaw * RECENCY_WEIGHT;

        double totalScore = BASE_SCORE
                + skillScore
                + experienceScore
                + locationScore
                + jobTypeScore
                + salaryScore
                + trendingScore
                + recencyScore;

        score.setBaseScore(round2(BASE_SCORE));
        score.setSkillScore(round2(skillScore));
        score.setExperienceScore(round2(experienceScore));
        score.setLocationScore(round2(locationScore));
        score.setJobTypeScore(round2(jobTypeScore));
        score.setSalaryScore(round2(salaryScore));
        score.setTrendingScore(round2(trendingScore));
        score.setRecencyScore(round2(recencyScore));
        score.setTotalScore(round2(totalScore));

        return score;
    }

    private double calculateSkillRawScore(CandidateProfile candidate, Job job) {
        List<String> candidateSkills = candidate.getSkillsList();
        List<String> jobSkills = job.getRequiredSkillsList();

        if (jobSkills == null || jobSkills.isEmpty()) {
            return 50.0;
        }

        if (candidateSkills == null || candidateSkills.isEmpty()) {
            return 5.0;
        }

        long exactMatches = 0;
        long partialMatches = 0;

        for (String jobSkill : jobSkills) {
            String normalizedJobSkill = normalize(jobSkill);

            boolean exact = candidateSkills.stream()
                    .map(this::normalize)
                    .anyMatch(cs -> cs.equals(normalizedJobSkill));

            if (exact) {
                exactMatches++;
                continue;
            }

            boolean partial = candidateSkills.stream()
                    .map(this::normalize)
                    .anyMatch(cs -> cs.contains(normalizedJobSkill) || normalizedJobSkill.contains(cs));

            if (partial) {
                partialMatches++;
            }
        }

        double raw = ((exactMatches + (partialMatches * 0.5)) / jobSkills.size()) * 100.0;
        return clamp(raw, 0.0, 100.0);
    }

    private double calculateExperienceRawScore(CandidateProfile candidate, Job job) {
        Integer candidateExp = candidate.getTotalExperienceYears();
        if (candidateExp == null) {
            return 15.0;
        }

        switch (job.getExperienceLevel()) {
            case INTERN:
                if (candidateExp <= 0.5) return 100.0;
                if (candidateExp <= 1) return 66.67;
                if (candidateExp <= 2) return 33.33;
                return 16.67;

            case JUNIOR_LEVEL:
                if (candidateExp <= 2) return 100.0;
                if (candidateExp <= 3) return 66.67;
                if (candidateExp <= 4) return 33.33;
                return 16.67;

            case MID_LEVEL:
                if (candidateExp >= 2 && candidateExp <= 5) return 100.0;
                if (candidateExp >= 1 && candidateExp < 2) return 66.67;
                if (candidateExp > 5 && candidateExp <= 7) return 50.0;
                if (candidateExp < 1) return 16.67;
                return 33.33;

            case SENIOR_LEVEL:
                if (candidateExp >= 5 && candidateExp <= 10) return 100.0;
                if (candidateExp >= 3 && candidateExp < 5) return 66.67;
                if (candidateExp > 10) return 83.33;
                if (candidateExp >= 1 && candidateExp < 3) return 33.33;
                return 16.67;

            case EXECUTIVE:
                if (candidateExp >= 10) return 100.0;
                if (candidateExp >= 8) return 83.33;
                if (candidateExp >= 5) return 66.67;
                if (candidateExp >= 3) return 50.0;
                return 33.33;

            default:
                return 15.0;
        }
    }

    private double calculateLocationRawScore(CandidateProfile candidate, Job job) {
        if (job.isRemote()) {
            if (candidate.getRemotePreference() == null) return 100.0;
            return candidate.getRemotePreference() ? 100.0 : 50.0;
        }

        List<String> preferredLocations = candidate.getPreferredLocationsList();
        if (preferredLocations == null || preferredLocations.isEmpty()) {
            return 0.0;
        }

        boolean locationMatch = preferredLocations.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .anyMatch(loc -> job.getLocation() != null &&
                        job.getLocation().toLowerCase().contains(loc.toLowerCase()));

        return locationMatch ? 100.0 : 0.0;
    }

    private double calculateJobTypeRawScore(CandidateProfile candidate, Job job) {
        List<String> preferredJobTypes = candidate.getPreferredJobTypesList();
        if (preferredJobTypes == null || preferredJobTypes.isEmpty()) {
            return 50.0;
        }

        boolean jobTypeMatch = preferredJobTypes.stream()
                .filter(Objects::nonNull)
                .anyMatch(type -> type.equalsIgnoreCase(job.getJobType().name()));

        return jobTypeMatch ? 100.0 : 0.0;
    }

    private double calculateSalaryRawScore(CandidateProfile candidate, Job job) {
        BigDecimal expectedSalary = candidate.getExpectedSalary();
        BigDecimal salaryMin = job.getSalaryMin();
        BigDecimal salaryMax = job.getSalaryMax();

        if (expectedSalary == null || salaryMin == null || salaryMax == null) {
            return 7.5;
        }

        if (expectedSalary.compareTo(salaryMax) <= 0 && expectedSalary.compareTo(salaryMin) >= 0) {
            return 100.0;
        }

        BigDecimal maxWithBuffer = salaryMax.multiply(BigDecimal.valueOf(1.20));
        if (expectedSalary.compareTo(salaryMax) > 0 && expectedSalary.compareTo(maxWithBuffer) <= 0) {
            return 50.0;
        }

        if (expectedSalary.compareTo(salaryMin) < 0) {
            return 100.0;
        }

        return 0.0;
    }

    private double calculateTrendingRawScore(Job job) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(TRENDING_WINDOW_DAYS);
        long applicationCount = jobApplicationRepository.countByJobIdAndAppliedAtAfter(job.getId(), startDate);

        if (applicationCount >= 50) return 100.0;
        if (applicationCount >= 30) return 75.0;
        if (applicationCount >= 15) return 50.0;
        if (applicationCount >= 5) return 25.0;
        return 12.5;
    }

    private double calculateRecencyRawScore(Job job) {
        if (job.getCreatedAt() == null) {
            return 20.0;
        }

        long daysOld = Duration.between(job.getCreatedAt(), LocalDateTime.now()).toDays();

        if (daysOld <= 1) return 70.0;
        if (daysOld <= 3) return 55.0;
        if (daysOld <= 7) return 40.0;
        if (daysOld <= 14) return 30.0;
        if (daysOld <= 30) return 20.0;
        return 10.0;
    }

    private JobRecommendationResponse createRecommendationResponse(
            Job job, CandidateProfile candidate, RecommendationScore score) {

        JobRecommendationResponse response = new JobRecommendationResponse();

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

        response.setSalaryMin(job.getSalaryMin());
        response.setSalaryMax(job.getSalaryMax());
        response.setSalaryCurrency(job.getSalaryCurrency());

        response.setRecommendationScore(score.getTotalScore());
        response.setMatchScore((int) Math.round(score.getTotalScore()));

        Map<String, Double> scoreBreakdown = new LinkedHashMap<>();
        scoreBreakdown.put("Base Score", score.getBaseScore());
        scoreBreakdown.put("Skills", score.getSkillScore());
        scoreBreakdown.put("Experience", score.getExperienceScore());
        scoreBreakdown.put("Location", score.getLocationScore());
        scoreBreakdown.put("Job Type", score.getJobTypeScore());
        scoreBreakdown.put("Salary", score.getSalaryScore());
        scoreBreakdown.put("Trending", score.getTrendingScore());
        scoreBreakdown.put("Recency", score.getRecencyScore());
        response.setScoreBreakdown(scoreBreakdown);

        response.setRecommendationReasons(generateRecommendationReasons(candidate, job));

        return response;
    }

    private List<String> generateRecommendationReasons(CandidateProfile candidate, Job job) {
        List<String> reasons = new ArrayList<>();

        List<String> matchingSkills = skillMatchService.findMatchingSkills(
                candidate.getSkillsList(), job.getRequiredSkillsList());

        if (!matchingSkills.isEmpty()) {
            if (matchingSkills.size() >= 5) {
                reasons.add("Excellent skill match: " + matchingSkills.size() + " skills match");
            } else if (matchingSkills.size() >= 3) {
                reasons.add("Good skill match: " + matchingSkills.size() + " key skills match");
            } else {
                reasons.add("Matches your skills in: " + String.join(", ",
                        matchingSkills.subList(0, Math.min(3, matchingSkills.size()))));
            }
        } else if (!job.getRequiredSkillsList().isEmpty()) {
            reasons.add("Consider adding these skills: " + String.join(", ",
                    job.getRequiredSkillsList().stream().limit(3).collect(Collectors.toList())));
        }

        if (candidate.getTotalExperienceYears() != null) {
            String expLevel = job.getExperienceLevel().name().toLowerCase().replace("_", " ");
            reasons.add("Your " + candidate.getTotalExperienceYears() + " years of experience (" + expLevel + " level)");
        }

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

        List<String> preferredJobTypes = candidate.getPreferredJobTypesList();
        if (preferredJobTypes != null && preferredJobTypes.contains(job.getJobType().name())) {
            reasons.add("Matches your preferred job type: " + job.getJobType().name());
        }

        if (candidate.getExpectedSalary() != null &&
                job.getSalaryMin() != null && job.getSalaryMax() != null) {
            if (candidate.getExpectedSalary().compareTo(job.getSalaryMax()) <= 0) {
                reasons.add("Salary range matches your expectations");
            } else if (candidate.getExpectedSalary().compareTo(job.getSalaryMin()) <= 0) {
                reasons.add("Salary is within your expected range");
            }
        }

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

        return candidate.getCompletionPercentage() >= 60
                && candidate.getUser().isActive()
                && candidate.getUser().isEmailVerified();
    }

    private String normalize(String value) {
        if (value == null) return "";
        return value.toLowerCase().replaceAll("[^a-z0-9]", "");
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}