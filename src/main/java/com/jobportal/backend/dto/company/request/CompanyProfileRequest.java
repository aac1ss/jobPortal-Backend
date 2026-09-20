package com.jobportal.backend.dto.company.request;

import com.jobportal.backend.enums.IndustryEnum;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CompanyProfileRequest {

    @NotBlank(message = "Company name is required")
    @Size(min = 2, max = 200, message = "Company name must be between 2 and 200 characters")
    private String companyName;

    @Size(max = 200, message = "Display name must not exceed 200 characters")
    private String displayName;

    @NotNull(message = "Industry is required")
    private IndustryEnum industry;

    @NotBlank(message = "Company size is required")
    private String companySize;

    @Size(max = 255, message = "Website URL is too long")
    private String website;

    @NotBlank(message = "Description is required")
    @Size(min = 100, max = 5000, message = "Description must be between 100 and 5000 characters")
    private String description;

    @NotBlank(message = "Contact name is required")
    @Size(min = 2, max = 100, message = "Contact name must be between 2 and 100 characters")
    private String contactName;

    @NotBlank(message = "Contact email is required")
    @Email(message = "Please provide a valid email address")
    private String contactEmail;

    @Size(max = 20, message = "Phone number is too long")
    private String contactPhone;

    private String address;

    @NotBlank(message = "City is required")
    @Size(max = 100, message = "City name is too long")
    private String city;

    @Size(max = 100, message = "State name is too long")
    private String state;

    @NotBlank(message = "Country is required")
    @Size(max = 100, message = "Country name is too long")
    private String country;

    @Size(max = 20, message = "Postal code is too long")
    private String postalCode;

    @Size(max = 500, message = "Logo URL is too long")
    private String logoUrl;

    @Size(max = 500, message = "Cover image URL is too long")
    private String coverImageUrl;

    @Size(max = 255, message = "LinkedIn URL is too long")
    private String linkedinUrl;

    @Size(max = 255, message = "Twitter URL is too long")
    private String twitterUrl;

    @Size(max = 255, message = "Facebook URL is too long")
    private String facebookUrl;
}