// dto/recruiter/response/CandidateProfileViewResponse.java
package com.jobportal.backend.dto.recruiter.response;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
public class CandidateProfileViewResponse {
    private Long candidateId;
    private String fullName;
    private String email;
    private String phone;
    private String profilePictureUrl;
    private String headline;
    private String summary;
    private Integer totalExperienceYears;
    private String currentSalary;
    private String expectedSalary;
    private String salaryCurrency;
    private List<ExperienceView> experiences;
    private List<EducationView> educations;
    private List<SkillView> skills;
    private List<CertificationView> certifications;
    private List<LanguageView> languages;
    private String resumeUrl;
    private String resumeFileName;
    private List<String> preferredJobTypes;
    private List<String> preferredLocations;
    private Boolean isActivelyLooking;

    @Data
    public static class ExperienceView {
        private String company;
        private String position;
        private String employmentType;
        private String location;
        private Boolean isCurrent;
        private LocalDate startDate;
        private LocalDate endDate;
        private String description;
        private List<String> achievements;
        private List<String> skillsUsed;
    }

    @Data
    public static class EducationView {
        private String institution;
        private String degree;
        private String fieldOfStudy;
        private String grade;
        private LocalDate startDate;
        private LocalDate endDate;
        private Boolean isCurrent;
        private String description;
    }

    @Data
    public static class SkillView {
        private String name;
        private Integer yearsOfExperience;
        private String proficiencyLevel;
    }

    @Data
    public static class CertificationView {
        private String name;
        private String issuingOrganization;
        private LocalDate issueDate;
        private LocalDate expirationDate;
    }

    @Data
    public static class LanguageView {
        private String language;
        private String proficiency;
    }
}