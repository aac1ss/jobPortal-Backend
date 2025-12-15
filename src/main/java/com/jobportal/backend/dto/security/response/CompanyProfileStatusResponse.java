package com.jobportal.backend.dto.security.response;

import lombok.Data;

@Data
public class CompanyProfileStatusResponse {
    private boolean hasProfile;
    private boolean isComplete;
    private boolean isVerified;
    private int completionPercentage;
    private String[] missingFields;
    private String message;

    public CompanyProfileStatusResponse(boolean hasProfile, boolean isComplete,
                                        boolean isVerified, int completionPercentage,
                                        String[] missingFields) {
        this.hasProfile = hasProfile;
        this.isComplete = isComplete;
        this.isVerified = isVerified;
        this.completionPercentage = completionPercentage;
        this.missingFields = missingFields;

        if (!hasProfile) {
            this.message = "No company profile found. Please create one.";
        } else if (!isComplete) {
            this.message = "Company profile is incomplete. Please complete it to post jobs.";
        } else if (!isVerified) {
            this.message = "Company profile is complete but not yet verified.";
        } else {
            this.message = "Company profile is complete and verified.";
        }
    }
}
