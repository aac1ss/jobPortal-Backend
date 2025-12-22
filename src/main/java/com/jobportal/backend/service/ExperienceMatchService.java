package com.jobportal.backend.service;

import com.jobportal.backend.enums.ExperienceLevel;
import org.springframework.stereotype.Service;

@Service
public class ExperienceMatchService {

    public int calculateExperienceMatchScore(Float candidateExperienceYears, ExperienceLevel requiredLevel) {
        if (candidateExperienceYears == null || requiredLevel == null) {
            return 15; // Default score if data missing
        }

        double candidateExpYears = candidateExperienceYears;

        switch (requiredLevel) {
            case INTERN:
                return matchInternExperience(candidateExpYears);
            case JUNIOR_LEVEL:
                return matchJuniorExperience(candidateExpYears);
            case MID_LEVEL:
                return matchMidLevelExperience(candidateExpYears);
            case SENIOR_LEVEL:
                return matchSeniorExperience(candidateExpYears);
            case EXECUTIVE:
                return matchExecutiveExperience(candidateExpYears);
            default:
                return 15;
        }
    }

    private int matchInternExperience(double years) {
        if (years <= 0.5) return 30;
        if (years <= 1) return 20;
        if (years <= 2) return 10;
        return 5;
    }

    private int matchJuniorExperience(double years) {
        if (years <= 2) return 30;
        if (years <= 3) return 20;
        if (years <= 4) return 10;
        return 5;
    }

    private int matchMidLevelExperience(double years) {
        if (years >= 2 && years <= 5) return 30;
        if (years >= 1 && years < 2) return 20;
        if (years > 5 && years <= 7) return 15;
        if (years < 1) return 5;
        return 10;
    }

    private int matchSeniorExperience(double years) {
        if (years >= 5 && years <= 10) return 30;
        if (years >= 3 && years < 5) return 20;
        if (years > 10) return 25;
        if (years >= 1 && years < 3) return 10;
        return 5;
    }

    private int matchExecutiveExperience(double years) {
        if (years >= 10) return 30;
        if (years >= 8) return 25;
        if (years >= 5) return 20;
        if (years >= 3) return 15;
        return 10;
    }
}