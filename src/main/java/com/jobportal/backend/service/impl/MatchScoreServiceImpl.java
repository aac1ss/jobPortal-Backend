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
        log.info("========== STARTING MATCH SCORE CALCULATION ==========");
        log.info("Candidate ID: {}, Job ID: {}, Job Title: {}",
                candidate.getId(), job.getId(), job.getTitle());
        log.info("Candidate Name: {}, Skills: {}",
                candidate.getFullName(), candidate.getSkillsList());

        int totalScore = 0;

        try {
            // 1. Skill Match (40 points max)
            List<String> candidateSkills = candidate.getSkillsList();
            List<String> jobSkills = job.getRequiredSkillsList();

            log.info("---------- SKILL MATCH (Max 40 points) ----------");
            log.info("Candidate Skills: {}", candidateSkills);
            log.info("Job Required Skills: {}", jobSkills);

            int skillScore = skillMatchService.calculateSkillMatchScore(candidateSkills, jobSkills);
            totalScore += skillScore;

            log.info("✓ Skill Score: {}/40 points", skillScore);
            log.info("  - Percentage: {}%", (skillScore * 100 / 40));

            // Calculate and log skill match details
            if (candidateSkills != null && jobSkills != null) {
                List<String> matchingSkills = skillMatchService.findMatchingSkills(candidateSkills, jobSkills);
                List<String> missingSkills = jobSkills.stream()
                        .filter(skill -> !candidateSkills.contains(skill))
                        .collect(Collectors.toList());
                log.info("  - Matching skills: {}", matchingSkills);
                log.info("  - Missing skills: {}", missingSkills);
                log.info("  - Match rate: {}/{} ({:.1f}%)",
                        matchingSkills.size(), jobSkills.size(),
                        (matchingSkills.size() * 100.0 / jobSkills.size()));
            }

            // 2. Experience Match (30 points max)
            Integer experienceYears = candidate.getTotalExperienceYears();
            log.info("---------- EXPERIENCE MATCH (Max 30 points) ----------");
            log.info("Candidate Experience: {} years", experienceYears);
            log.info("Job Required Level: {}", job.getExperienceLevel());

            Float candidateExperience = experienceYears != null ? experienceYears.floatValue() : 0f;
            int experienceScore = experienceMatchService.calculateExperienceMatchScore(
                    candidateExperience, job.getExperienceLevel()
            );
            totalScore += experienceScore;

            log.info("✓ Experience Score: {}/30 points", experienceScore);
            log.info("  - Percentage: {}%", (experienceScore * 100 / 30));

            // Experience level analysis
            String experienceAssessment = getExperienceAssessment(experienceYears, job.getExperienceLevel());
            log.info("  - Assessment: {}", experienceAssessment);

            // 3. Location Match (20 points max)
            log.info("---------- LOCATION MATCH (Max 20 points) ----------");
            log.info("Job Location: {}", job.getLocation());
            log.info("Is Remote Job: {}", job.isRemote());
            log.info("Candidate Preferred Locations: {}", candidate.getPreferredLocationsList());
            log.info("Candidate Remote Preference: {}", candidate.getRemotePreference());

            int locationScore = locationMatchService.calculateLocationMatchScore(candidate, job);
            totalScore += locationScore;

            log.info("✓ Location Score: {}/20 points", locationScore);
            log.info("  - Percentage: {}%", (locationScore * 100 / 20));

            // Location match analysis
            if (job.isRemote()) {
                if (Boolean.TRUE.equals(candidate.getRemotePreference())) {
                    log.info("  - Analysis: Perfect match - Remote job matches candidate's remote preference");
                } else if (candidate.getRemotePreference() == null) {
                    log.info("  - Analysis: Partial match - Remote job but candidate remote preference not set");
                } else {
                    log.info("  - Analysis: Low match - Remote job but candidate prefers office work");
                }
            } else {
                boolean locationMatches = candidate.getPreferredLocationsList() != null &&
                        candidate.getPreferredLocationsList().stream()
                                .anyMatch(loc -> job.getLocation() != null &&
                                        job.getLocation().toLowerCase().contains(loc.toLowerCase()));
                if (locationMatches) {
                    log.info("  - Analysis: Location matches candidate preference");
                } else {
                    log.info("  - Analysis: Location does NOT match candidate preference");
                }
            }

            // 4. Job Type Match (10 points max)
            log.info("---------- JOB TYPE MATCH (Max 10 points) ----------");
            log.info("Job Type: {}", job.getJobType());
            log.info("Candidate Preferred Job Types: {}", candidate.getPreferredJobTypesList());

            int jobTypeScore = jobTypeMatchService.calculateJobTypeMatchScore(candidate, job);
            totalScore += jobTypeScore;

            log.info("✓ Job Type Score: {}/10 points", jobTypeScore);
            log.info("  - Percentage: {}%", (jobTypeScore * 100 / 10));

            if (candidate.getPreferredJobTypesList() != null &&
                    candidate.getPreferredJobTypesList().contains(job.getJobType().name())) {
                log.info("  - Analysis: Job type matches candidate preference");
            } else {
                log.info("  - Analysis: Job type does NOT match candidate preference");
            }

            // Final Score Summary
            log.info("========== FINAL MATCH SCORE SUMMARY ==========");
            log.info("┌─────────────────────────────────────────────────┐");
            log.info("│ Component          | Score    | Max    | %      │");
            log.info("├─────────────────────────────────────────────────┤");
            log.info("│ Skills             | {:3d}      | 40     | {:3d}%     │", skillScore, (skillScore * 100 / 40));
            log.info("│ Experience         | {:3d}      | 30     | {:3d}%     │", experienceScore, (experienceScore * 100 / 30));
            log.info("│ Location           | {:3d}      | 20     | {:3d}%     │", locationScore, (locationScore * 100 / 20));
            log.info("│ Job Type           | {:3d}      | 10     | {:3d}%     │", jobTypeScore, (jobTypeScore * 100 / 10));
            log.info("├─────────────────────────────────────────────────┤");
            log.info("│ TOTAL              | {:3d}      | 100    | {:3d}%     │", totalScore, totalScore);
            log.info("└─────────────────────────────────────────────────┘");

            // Grade based on total score
            String grade;
            if (totalScore >= 80) grade = "A (Excellent)";
            else if (totalScore >= 70) grade = "B (Very Good)";
            else if (totalScore >= 60) grade = "C (Good)";
            else if (totalScore >= 50) grade = "D (Average)";
            else if (totalScore >= 40) grade = "E (Below Average)";
            else grade = "F (Poor)";

            log.info("Overall Grade: {}", grade);
            log.info("=================================================");

        } catch (Exception e) {
            log.error("Error calculating match score for candidate {} and job {}",
                    candidate.getId(), job.getId(), e);
            log.error("Returning safe default score: {}", totalScore);
            // Return a safe default score
            return Math.min(totalScore, 100);
        }

        // Ensure score is between 0 and 100
        int finalScore = Math.min(Math.max(totalScore, 0), 100);
        log.info("Final validated score (clamped between 0-100): {}", finalScore);
        return finalScore;
    }

    @Override
    public String generateMatchNotes(CandidateProfile candidate, Job job, int score) {
        log.debug("Generating match notes for candidate {} and job {} with score {}",
                candidate.getId(), job.getId(), score);

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
            log.debug("Found {} matching skills: {}", matchingSkills.size(), matchingSkills);
        } else {
            notes.add("No direct skill matches were identified.");
            log.debug("No matching skills found");
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
                log.debug("Experience suitable: {} years vs required {}", candidateExp, requiredLevel);
            } else {
                notes.add(String.format(
                        "Candidate experience: %.1f years. This may not fully align with the required level (%s).",
                        candidateExp,
                        requiredLevel
                ));
                log.debug("Experience not suitable: {} years vs required {}", candidateExp, requiredLevel);
            }
        } else {
            notes.add("Candidate experience information is not available.");
            log.debug("No experience information available for candidate");
        }

        // Location note - align with actual location scoring
        int locationScore = locationMatchService.calculateLocationMatchScore(candidate, job);
        if (job.isRemote()) {
            notes.add("This job is remote.");
            log.debug("Job is remote, location score: {}", locationScore);
        } else if (locationScore > 0) {
            notes.add("Candidate location preference matches this job.");
            log.debug("Location preference matches, score: {}", locationScore);
        } else {
            notes.add("Candidate location preference does not fully match this job.");
            log.debug("Location preference does not match, score: {}", locationScore);
        }

        // Job type note - recruiter facing
        int jobTypeScore = jobTypeMatchService.calculateJobTypeMatchScore(candidate, job);
        String jobTypeLabel = formatEnumLabel(job.getJobType() != null ? job.getJobType().name() : null);

        if (jobTypeScore > 0) {
            notes.add("Candidate preferred job type matches this role (" + jobTypeLabel + ").");
            log.debug("Job type matches: {} vs preferred {}", jobTypeLabel, candidate.getPreferredJobTypesList());
        } else {
            notes.add("Candidate preferred job type does not fully match this role (" + jobTypeLabel + ").");
            log.debug("Job type does not match: {} vs preferred {}", jobTypeLabel, candidate.getPreferredJobTypesList());
        }

        String finalNotes = String.join(" ", notes);
        log.debug("Generated match notes: {}", finalNotes);

        return finalNotes;
    }

    private boolean isExperienceSuitable(Float candidateExp, com.jobportal.backend.enums.ExperienceLevel requiredLevel) {
        if (candidateExp == null || requiredLevel == null) return true;

        double years = candidateExp;
        boolean suitable;

        switch (requiredLevel) {
            case INTERN:
                suitable = years <= 2;
                log.debug("Experience check - Intern level: {} years <= 2? {}", years, suitable);
                break;
            case JUNIOR_LEVEL:
                suitable = years <= 4;
                log.debug("Experience check - Junior level: {} years <= 4? {}", years, suitable);
                break;
            case MID_LEVEL:
                suitable = years >= 2 && years <= 7;
                log.debug("Experience check - Mid level: {} years between 2-7? {}", years, suitable);
                break;
            case SENIOR_LEVEL:
                suitable = years >= 3;
                log.debug("Experience check - Senior level: {} years >= 3? {}", years, suitable);
                break;
            case EXECUTIVE:
                suitable = years >= 5;
                log.debug("Experience check - Executive level: {} years >= 5? {}", years, suitable);
                break;
            default:
                suitable = true;
                log.debug("Experience check - Unknown level, defaulting to suitable");
        }
        return suitable;
    }

    private String getExperienceAssessment(Integer experienceYears, com.jobportal.backend.enums.ExperienceLevel requiredLevel) {
        if (experienceYears == null || requiredLevel == null) {
            return "Experience information incomplete";
        }

        double years = experienceYears;
        switch (requiredLevel) {
            case INTERN:
                if (years <= 0.5) return "Perfect for internship (fresh graduate)";
                if (years <= 2) return "Acceptable for internship (some experience)";
                return "Overqualified for internship";
            case JUNIOR_LEVEL:
                if (years <= 2) return "Ideal for junior position";
                if (years <= 4) return "Acceptable for junior position";
                return "Overqualified for junior position";
            case MID_LEVEL:
                if (years >= 3 && years <= 5) return "Ideal for mid-level position";
                if (years >= 2 && years <= 7) return "Acceptable for mid-level position";
                if (years < 2) return "Underqualified for mid-level position";
                return "Overqualified for mid-level position";
            case SENIOR_LEVEL:
                if (years >= 5 && years <= 8) return "Ideal for senior position";
                if (years >= 3 && years <= 10) return "Acceptable for senior position";
                if (years < 3) return "Underqualified for senior position";
                return "Highly experienced for senior position";
            case EXECUTIVE:
                if (years >= 8) return "Ideal for executive position";
                if (years >= 5) return "Acceptable for executive position";
                return "Underqualified for executive position";
            default:
                return "Experience level unknown";
        }
    }

    private String formatEnumLabel(String value) {
        if (value == null || value.isBlank()) return "N/A";
        return value.toLowerCase().replace("_", " ");
    }

    // Additional helper method for batch processing
    public List<JobMatchResult> findBestMatchingJobs(CandidateProfile candidate, List<Job> jobs, int limit) {
        log.info("Finding top {} best matching jobs for candidate {}", limit, candidate.getId());

        List<JobMatchResult> results = jobs.stream()
                .map(job -> {
                    int score = calculateMatchScore(candidate, job);
                    String notes = generateMatchNotes(candidate, job, score);
                    log.debug("Job '{}' - Match score: {}/100", job.getTitle(), score);
                    return new JobMatchResult(job, score, notes);
                })
                .sorted((a, b) -> b.getScore() - a.getScore())
                .limit(limit)
                .collect(Collectors.toList());

        log.info("Top {} jobs found. Best score: {}, Worst score: {}",
                results.size(),
                results.isEmpty() ? 0 : results.get(0).getScore(),
                results.isEmpty() ? 0 : results.get(results.size() - 1).getScore());

        return results;
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