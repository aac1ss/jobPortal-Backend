package com.jobportal.backend.service.impl;

import com.jobportal.backend.entity.CandidateProfile;
import com.jobportal.backend.entity.Job;
import com.jobportal.backend.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchScoreServiceImpl implements MatchScoreService {

    private final SkillMatchService skillMatchService;
    private final ExperienceMatchService experienceMatchService;
    private final LocationMatchService locationMatchService;
    private final JobTypeMatchService jobTypeMatchService;

    @Override
    public int calculateMatchScore(CandidateProfile candidate, Job job) {
        int totalScore = 0;

        try {
            // 1. Skill Match (40 points max)
            List<String> candidateSkills = candidate.getSkillsList();
            List<String> jobSkills = job.getRequiredSkillsList();
            int skillScore = skillMatchService.calculateSkillMatchScore(candidateSkills, jobSkills);
            totalScore += skillScore;

            // 2. Experience Match (30 points max)
            Float candidateExperience = Float.valueOf(candidate.getTotalExperienceYears());
            int experienceScore = experienceMatchService.calculateExperienceMatchScore(
                    candidateExperience, job.getExperienceLevel()
            );
            totalScore += experienceScore;

            // 3. Location Match (20 points max)
            int locationScore = locationMatchService.calculateLocationMatchScore(candidate, job);
            totalScore += locationScore;

            // 4. Job Type Match (10 points max)
            int jobTypeScore = jobTypeMatchService.calculateJobTypeMatchScore(candidate, job);
            totalScore += jobTypeScore;

        } catch (Exception e) {
            log.error("Error calculating match score for candidate {} and job {}",
                    candidate.getId(), job.getId(), e);
            // Return a safe default score
            return Math.min(totalScore, 100);
        }

        // Ensure score is between 0 and 100
        return Math.min(Math.max(totalScore, 0), 100);
    }

    @Override
    public String generateMatchNotes(CandidateProfile candidate, Job job, int score) {
        List<String> notes = new ArrayList<>();

        // Overall assessment - recruiter facing
        if (score >= 80) {
            notes.add("Excellent fit. This candidate aligns strongly with the role requirements.");
        } else if (score >= 60) {
            notes.add("Good fit. This candidate shows strong relevance for the position.");
        } else if (score >= 40) {
            notes.add("Moderate fit. The candidate matches some key requirements but may need review.");
        } else {
            notes.add("Basic fit. The candidate may require additional skills or experience for this role.");
        }

        // Skills note
        List<String> matchingSkills = skillMatchService.findMatchingSkills(
                candidate.getSkillsList(), job.getRequiredSkillsList()
        );

        if (!matchingSkills.isEmpty()) {
            notes.add("Candidate matching skills: " + String.join(", ", matchingSkills) + ".");
        } else {
            notes.add("No direct skill matches were identified.");
        }

        // Experience note
        Integer totalExperienceYears = candidate.getTotalExperienceYears();
        if (totalExperienceYears != null) {
            Float candidateExp = totalExperienceYears.floatValue();
            String requiredLevel = formatEnumLabel(job.getExperienceLevel() != null ? job.getExperienceLevel().name() : null);

            if (isExperienceSuitable(candidateExp, job.getExperienceLevel())) {
                notes.add(String.format(
                        "Candidate experience: %.1f years. This aligns with the required level (%s).",
                        candidateExp,
                        requiredLevel
                ));
            } else {
                notes.add(String.format(
                        "Candidate experience: %.1f years. This may not fully align with the required level (%s).",
                        candidateExp,
                        requiredLevel
                ));
            }
        } else {
            notes.add("Candidate experience information is not available.");
        }

        // Location note - align with actual location scoring
        int locationScore = locationMatchService.calculateLocationMatchScore(candidate, job);
        if (job.isRemote()) {
            notes.add("This job is remote.");
        } else if (locationScore > 0) {
            notes.add("Candidate location preference matches this job.");
        } else {
            notes.add("Candidate location preference does not fully match this job.");
        }

        // Job type note - recruiter facing
        int jobTypeScore = jobTypeMatchService.calculateJobTypeMatchScore(candidate, job);
        String jobTypeLabel = formatEnumLabel(job.getJobType() != null ? job.getJobType().name() : null);

        if (jobTypeScore > 0) {
            notes.add("Candidate preferred job type matches this role (" + jobTypeLabel + ").");
        } else {
            notes.add("Candidate preferred job type does not fully match this role (" + jobTypeLabel + ").");
        }

        return String.join(" ", notes);
    }

    private boolean isExperienceSuitable(Float candidateExp, com.jobportal.backend.enums.ExperienceLevel requiredLevel) {
        if (candidateExp == null || requiredLevel == null) return true;

        double years = candidateExp;
        switch (requiredLevel) {
            case INTERN: return years <= 2;
            case JUNIOR_LEVEL: return years <= 4;
            case MID_LEVEL: return years >= 2 && years <= 7;
            case SENIOR_LEVEL: return years >= 3;
            case EXECUTIVE: return years >= 5;
            default: return true;
        }
    }
    private String formatEnumLabel(String value) {
        if (value == null || value.isBlank()) return "N/A";
        return value.toLowerCase().replace("_", " ");
    }

    // Additional helper method for batch processing
    public List<JobMatchResult> findBestMatchingJobs(CandidateProfile candidate, List<Job> jobs, int limit) {
        return jobs.stream()
                .map(job -> {
                    int score = calculateMatchScore(candidate, job);
                    String notes = generateMatchNotes(candidate, job, score);
                    return new JobMatchResult(job, score, notes);
                })
                .sorted((a, b) -> b.getScore() - a.getScore())
                .limit(limit)
                .collect(Collectors.toList());
    }

    // DTO for job match results
    public static class JobMatchResult {
        private final Job job;
        private final int score;
        private final String notes;

        public JobMatchResult(Job job, int score, String notes) {
            this.job = job;
            this.score = score;
            this.notes = notes;
        }

        public Job getJob() { return job; }
        public int getScore() { return score; }
        public String getNotes() { return notes; }
    }
}
