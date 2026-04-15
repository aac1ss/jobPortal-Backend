package com.jobportal.backend.dto.candidate.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.jobportal.backend.enums.Gender;
import com.jobportal.backend.enums.NoticePeriod;
import com.jobportal.backend.enums.ProfileVisibility;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CandidateProfileRequest {

    // Personal Information
    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 100, message = "First name must be between 2 and 100 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 100, message = "Last name must be between 2 and 100 characters")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @Pattern(regexp = "^[+]?[0-9]{10,15}$", message = "Invalid phone number format")
    private String phone;

    @Past(message = "Date of birth must be in the past")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateOfBirth;

    private Gender gender;

    private String address;
    private String city;
    private String country;

    @Size(max = 500, message = "Profile picture URL is too long")
    private String profilePictureUrl;

    // Professional Information
    @Size(max = 200, message = "Headline must be less than 200 characters")
    private String headline;

    @Size(min = 50, max = 2000, message = "Summary must be between 50 and 2000 characters")
    private String summary;

    @Min(value = 0, message = "Experience cannot be negative")
    @Max(value = 50, message = "Experience cannot exceed 50 years")
    private Integer totalExperienceYears;

    @PositiveOrZero(message = "Current salary must be positive or zero")
    private BigDecimal currentSalary;

    @PositiveOrZero(message = "Expected salary must be positive or zero")
    private BigDecimal expectedSalary;

    @Size(min = 3, max = 3, message = "Currency code must be 3 characters")
    private String salaryCurrency;

    private NoticePeriod noticePeriod;

    // Collections
    private List<EducationRequest> education;
    private List<ExperienceRequest> experience;
    private List<String> skills;
    private List<CertificationRequest> certifications;
    private List<LanguageRequest> languages;

    // Job Preferences
    private List<String> preferredJobTypes;
    private List<String> preferredLocations;
    private List<String> preferredIndustries;
    private Boolean remotePreference;

    // Settings
    private ProfileVisibility profileVisibility;
    private Boolean isActivelyLooking;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate availableFrom;

}