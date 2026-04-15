package com.jobportal.backend.dto.company.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CompanyProfileResponse {
    private Long id;
    private String companyName;
    private String displayName;

    // Industry information
    private Long industryId;
    private String industryName;

    private String companySize;
    private String website;
    private String description;
    private String contactName;
    private String contactEmail;
    private String contactPhone;
    private String address;
    private String city;
    private String state;
    private String country;
    private String postalCode;
    private String logoUrl;
    private String coverImageUrl;
    private boolean isVerified;
    private boolean isActive;
    private boolean profileComplete;
    private int completionPercentage;
    private String linkedinUrl;
    private String twitterUrl;
    private String facebookUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime verifiedAt;
    private Long recruiterId;
    private String recruiterName;
    private String recruiterEmail;
}