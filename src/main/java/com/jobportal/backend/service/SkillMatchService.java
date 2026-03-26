package com.jobportal.backend.service;

import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SkillMatchService {

    public int calculateSkillMatchScore(List<String> candidateSkills, List<String> jobSkills) {
        if (candidateSkills == null || candidateSkills.isEmpty() ||
                jobSkills == null || jobSkills.isEmpty()) {
            return 0;
        }

        // Normalize skills for better matching
        Set<String> normalizedCandidateSkills = normalizeSkills(candidateSkills);
        Set<String> normalizedJobSkills = normalizeSkills(jobSkills);

        if (normalizedJobSkills.isEmpty()) {
            return 0;
        }

        // Calculate exact matches
        long exactMatches = normalizedCandidateSkills.stream()
                .filter(normalizedJobSkills::contains)
                .count();

        // Calculate partial matches
        long partialMatches = calculatePartialMatches(normalizedCandidateSkills, normalizedJobSkills);

        // Total matches with weight (exact matches are more valuable)
        double totalMatches = exactMatches + (partialMatches * 0.5);
        double skillMatchPercent = totalMatches / normalizedJobSkills.size();

        return (int) (skillMatchPercent * 40); // Skills contribute up to 40 points
    }

    private Set<String> normalizeSkills(List<String> skills) {
        return skills.stream()
                .filter(skill -> skill != null && !skill.trim().isEmpty())
                .map(String::toLowerCase)
                .map(skill -> skill.replaceAll("[^a-z0-9]", "")) // Remove special characters
                .collect(Collectors.toSet());
    }

    private long calculatePartialMatches(Set<String> candidateSkills, Set<String> jobSkills) {
        long partialMatches = 0;

        for (String candidateSkill : candidateSkills) {
            for (String jobSkill : jobSkills) {
                if (isPartialMatch(candidateSkill, jobSkill)) {
                    partialMatches++;
                    break; // Count only one partial match per candidate skill
                }
            }
        }

        return partialMatches;
    }

    private boolean isPartialMatch(String skill1, String skill2) {
        // Check if one contains the other (accounting for variations like "java" vs "java8")
        return skill1.contains(skill2) || skill2.contains(skill1) ||
                // Check for common variations
                skill1.replace(" ", "").contains(skill2) ||
                skill2.replace(" ", "").contains(skill1);
    }

    public List<String> findMatchingSkills(List<String> candidateSkills, List<String> jobSkills) {
        Set<String> normalizedCandidateSkills = normalizeSkills(candidateSkills);
        Set<String> normalizedJobSkills = normalizeSkills(jobSkills);

        return normalizedCandidateSkills.stream()
                .filter(normalizedJobSkills::contains)
                .collect(Collectors.toList());
    }
}