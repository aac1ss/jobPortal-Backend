package com.jobportal.backend.service;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SkillMatchService {

    private static final int MAX_SKILL_SCORE = 40;
    private static final Set<String> SAFE_SUFFIXES = Set.of("js", "api", "boot", "mvc", "jpa", "sql");

    public int calculateSkillMatchScore(List<String> candidateSkills, List<String> jobSkills) {
        return analyzeSkillMatches(candidateSkills, jobSkills).getScore();
    }

    public List<String> findMatchingSkills(List<String> candidateSkills, List<String> jobSkills) {
        return new ArrayList<>(analyzeSkillMatches(candidateSkills, jobSkills).getMatchedJobSkills());
    }

    public List<String> findMissingSkills(List<String> candidateSkills, List<String> jobSkills) {
        return new ArrayList<>(analyzeSkillMatches(candidateSkills, jobSkills).getMissingJobSkills());
    }

    private SkillMatchAnalysis analyzeSkillMatches(List<String> candidateSkills, List<String> jobSkills) {
        if (candidateSkills == null || candidateSkills.isEmpty() ||
                jobSkills == null || jobSkills.isEmpty()) {
            return SkillMatchAnalysis.empty();
        }

        Map<String, String> normalizedCandidateMap = toNormalizedSkillMap(candidateSkills);
        Map<String, String> normalizedJobMap = toNormalizedSkillMap(jobSkills);

        if (normalizedCandidateMap.isEmpty() || normalizedJobMap.isEmpty()) {
            return SkillMatchAnalysis.empty();
        }

        Set<String> usedCandidateSkills = new HashSet<>();
        Set<String> matchedJobSkills = new LinkedHashSet<>();
        Set<String> missingJobSkills = new LinkedHashSet<>();

        int exactMatches = 0;
        int partialMatches = 0;

        List<String> unmatchedJobSkills = new ArrayList<>();

        // Pass 1: exact matches only
        for (String normalizedJobSkill : normalizedJobMap.keySet()) {
            String matchedCandidate = findBestExactCandidate(
                    normalizedJobSkill,
                    normalizedCandidateMap.keySet(),
                    usedCandidateSkills
            );

            if (matchedCandidate != null) {
                exactMatches++;
                usedCandidateSkills.add(matchedCandidate);
                matchedJobSkills.add(normalizedJobMap.get(normalizedJobSkill));
            } else {
                unmatchedJobSkills.add(normalizedJobSkill);
            }
        }

        // Pass 2: partial matches only on remaining unmatched job skills
        for (String normalizedJobSkill : unmatchedJobSkills) {
            String matchedCandidate = findBestPartialCandidate(
                    normalizedJobSkill,
                    normalizedCandidateMap.keySet(),
                    usedCandidateSkills
            );

            if (matchedCandidate != null) {
                partialMatches++;
                usedCandidateSkills.add(matchedCandidate);
                matchedJobSkills.add(normalizedJobMap.get(normalizedJobSkill));
            } else {
                missingJobSkills.add(normalizedJobMap.get(normalizedJobSkill));
            }
        }

        double weightedMatches = exactMatches + (partialMatches * 0.5);
        double scoreRatio = weightedMatches / normalizedJobMap.size();

        int score = (int) Math.round(scoreRatio * MAX_SKILL_SCORE);
        score = Math.max(0, Math.min(MAX_SKILL_SCORE, score));

        return new SkillMatchAnalysis(
                score,
                exactMatches,
                partialMatches,
                matchedJobSkills,
                missingJobSkills
        );
    }

    private String findBestExactCandidate(String jobSkill,
                                          Set<String> candidateSkills,
                                          Set<String> usedCandidateSkills) {
        for (String candidateSkill : candidateSkills) {
            if (usedCandidateSkills.contains(candidateSkill)) {
                continue;
            }
            if (areExactEquivalent(candidateSkill, jobSkill)) {
                return candidateSkill;
            }
        }
        return null;
    }

    private String findBestPartialCandidate(String jobSkill,
                                            Set<String> candidateSkills,
                                            Set<String> usedCandidateSkills) {
        String bestCandidate = null;
        double bestScore = 0.0;

        for (String candidateSkill : candidateSkills) {
            if (usedCandidateSkills.contains(candidateSkill)) {
                continue;
            }

            double score = partialMatchScore(candidateSkill, jobSkill);
            if (score > bestScore) {
                bestScore = score;
                bestCandidate = candidateSkill;
            }
        }

        return bestCandidate;
    }

    private boolean areExactEquivalent(String skill1, String skill2) {
        return skill1.equals(skill2) || compact(skill1).equals(compact(skill2));
    }

    private boolean isPartialMatch(String skill1, String skill2) {
        return partialMatchScore(skill1, skill2) > 0.0;
    }

    private double partialMatchScore(String skill1, String skill2) {
        if (areExactEquivalent(skill1, skill2)) {
            return 0.0;
        }

        Set<String> tokens1 = tokenizeToSet(skill1);
        Set<String> tokens2 = tokenizeToSet(skill2);

        if (tokens1.isEmpty() || tokens2.isEmpty()) {
            return 0.0;
        }

        Set<String> shared = new HashSet<>(tokens1);
        shared.retainAll(tokens2);
        int sharedCount = shared.size();

        boolean oneSideSingleToken = tokens1.size() == 1 || tokens2.size() == 1;
        boolean oneSetContainsOther = tokens1.containsAll(tokens2) || tokens2.containsAll(tokens1);

        if (sharedCount >= 2) {
            return 2.0 + (sharedCount * 0.1);
        }

        if (sharedCount == 1 && oneSideSingleToken && oneSetContainsOther) {
            return 1.5;
        }

        if (hasSafeContainedVariant(skill1, skill2)) {
            return 1.25;
        }

        return 0.0;
    }

    private boolean hasSafeContainedVariant(String skill1, String skill2) {
        String compact1 = compact(skill1);
        String compact2 = compact(skill2);

        if (compact1.equals(compact2)) {
            return false;
        }

        return isSafePrefixVariant(compact1, compact2) || isSafePrefixVariant(compact2, compact1);
    }

    private boolean isSafePrefixVariant(String longer, String shorter) {
        if (!longer.startsWith(shorter)) {
            return false;
        }

        String suffix = longer.substring(shorter.length());
        if (suffix.isEmpty()) {
            return false;
        }

        boolean digitsOnly = suffix.chars().allMatch(Character::isDigit);
        return digitsOnly || SAFE_SUFFIXES.contains(suffix);
    }

    private Map<String, String> toNormalizedSkillMap(List<String> skills) {
        Map<String, String> normalizedMap = new LinkedHashMap<>();

        if (skills == null) {
            return normalizedMap;
        }

        for (String skill : skills) {
            String normalized = normalizeSkill(skill);
            if (!normalized.isEmpty()) {
                normalizedMap.putIfAbsent(normalized, skill.trim());
            }
        }

        return normalizedMap;
    }

    private String normalizeSkill(String skill) {
        if (skill == null) {
            return "";
        }

        String cleaned = skill.trim().toLowerCase(Locale.ROOT);
        if (cleaned.isEmpty()) {
            return "";
        }

        cleaned = cleaned.replaceAll("[^a-z0-9]+", " ").trim();
        if (cleaned.isEmpty()) {
            return "";
        }

        List<String> normalizedTokens = Arrays.stream(cleaned.split("\\s+"))
                .filter(token -> !token.isBlank())
                .map(this::normalizeToken)
                .filter(token -> !token.isBlank())
                .collect(Collectors.toList());

        return String.join(" ", normalizedTokens).trim();
    }

    private String normalizeToken(String token) {
        switch (token) {
            case "apis":
                return "api";
            case "services":
                return "service";
            case "microservices":
                return "microservice";
            case "databases":
                return "database";
            case "libraries":
                return "library";
            case "frameworks":
                return "framework";
            case "applications":
                return "application";
            case "developers":
                return "developer";
            case "engineers":
                return "engineer";
            case "pipelines":
                return "pipeline";
            case "containers":
                return "container";
            case "tests":
                return "test";
            case "testing":
                return "test";
            case "deployments":
                return "deployment";
            case "deploy":
                return "deployment";
            case "deploying":
                return "deployment";
            case "integrations":
                return "integration";
            case "authentications":
                return "authentication";
            case "authorizations":
                return "authorization";
            case "repositories":
                return "repository";
            case "queues":
                return "queue";
            case "messages":
                return "message";
            case "workers":
                return "worker";

            // useful tech wording normalization
            case "js":
                return "javascript";
            case "ts":
                return "typescript";
            case "nodejs":
                return "node";
            case "reactjs":
                return "react";
            case "vuejs":
                return "vue";
            case "nextjs":
                return "next";
            case "nuxtjs":
                return "nuxt";
            case "expressjs":
                return "express";
            case "nestjs":
                return "nestjs";
            case "restful":
                return "rest";
            case "restapis":
                return "rest api";
            case "graphqlapis":
                return "graphql api";
            case "postgres":
                return "postgresql";
            case "mongo":
                return "mongodb";
            case "k8s":
                return "kubernetes";
            case "ci":
                return "cicd";
            case "cd":
                return "cicd";
            case "ci/cd":
                return "cicd";
            case "devops":
                return "devops";

            default:
                return token;
        }
    }

    private Set<String> normalizeSkills(List<String> skills) {
        return toNormalizedSkillMap(skills).keySet();
    }

    private Set<String> tokenizeToSet(String normalizedSkill) {
        if (normalizedSkill == null || normalizedSkill.isBlank()) {
            return Collections.emptySet();
        }

        return Arrays.stream(normalizedSkill.split("\\s+"))
                .filter(token -> !token.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String compact(String normalizedSkill) {
        return normalizedSkill.replace(" ", "");
    }

    private long calculatePartialMatches(Set<String> candidateSkills, Set<String> jobSkills) {
        Set<String> usedCandidateSkills = new HashSet<>();
        long partialMatches = 0;

        for (String jobSkill : jobSkills) {
            String matchedCandidate = findBestPartialCandidate(jobSkill, candidateSkills, usedCandidateSkills);
            if (matchedCandidate != null) {
                partialMatches++;
                usedCandidateSkills.add(matchedCandidate);
            }
        }

        return partialMatches;
    }

    private static final class SkillMatchAnalysis {
        private final int score;
        private final int exactMatches;
        private final int partialMatches;
        private final Set<String> matchedJobSkills;
        private final Set<String> missingJobSkills;

        private SkillMatchAnalysis(int score,
                                   int exactMatches,
                                   int partialMatches,
                                   Set<String> matchedJobSkills,
                                   Set<String> missingJobSkills) {
            this.score = score;
            this.exactMatches = exactMatches;
            this.partialMatches = partialMatches;
            this.matchedJobSkills = matchedJobSkills;
            this.missingJobSkills = missingJobSkills;
        }

        static SkillMatchAnalysis empty() {
            return new SkillMatchAnalysis(
                    0,
                    0,
                    0,
                    new LinkedHashSet<>(),
                    new LinkedHashSet<>()
            );
        }

        public int getScore() {
            return score;
        }

        public int getExactMatches() {
            return exactMatches;
        }

        public int getPartialMatches() {
            return partialMatches;
        }

        public Set<String> getMatchedJobSkills() {
            return matchedJobSkills;
        }

        public Set<String> getMissingJobSkills() {
            return missingJobSkills;
        }
    }
}