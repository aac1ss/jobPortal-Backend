package com.jobportal.backend.dto.candidate.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.jobportal.backend.enums.Gender;
import com.jobportal.backend.enums.NoticePeriod;
import com.jobportal.backend.enums.ProfileVisibility;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CandidateProfileResponse {
    private Long id;
    private Long userId;
    private String firstName;
    private String lastName;
    private String fullName;
    private String email;
    private String phone;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateOfBirth;

    private Gender gender;
    private String address;
    private String city;
    private String country;
    private String profilePictureUrl;

    private String headline;
    private String summary;
    private Integer totalExperienceYears;
    private BigDecimal currentSalary;
    private BigDecimal expectedSalary;
    private String salaryCurrency;
    private NoticePeriod noticePeriod;

    private List<EducationResponse> education;
    private List<ExperienceResponse> experience;
    private List<String> skills;
    private List<CertificationResponse> certifications;
    private List<LanguageResponse> languages;

    private List<String> preferredJobTypes;
    private List<String> preferredLocations;
    private List<String> preferredIndustries;
    private Boolean remotePreference;

    private String resumeUrl;
    private String resumeFileName;
    private Boolean isResumeUploaded;
    private ProfileVisibility profileVisibility;
    private Boolean isProfileComplete;
    private Integer completionPercentage;
    private Boolean isFeatured;
    private Boolean isActivelyLooking;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate availableFrom;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastActiveAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    // Stats
    private Integer totalApplications;
    private Map<String, Integer> applicationStatusCount;

    // Eligibility
    private Boolean canApplyForJobs;
    private List<String> eligibilityReasons;

}
