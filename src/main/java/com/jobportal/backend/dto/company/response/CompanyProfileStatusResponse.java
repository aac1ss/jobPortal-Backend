package com.jobportal.backend.dto.company.response;

import lombok.Data;

@Data
public class CompanyProfileStatusResponse {
    private boolean hasProfile;
    private boolean isComplete;
    private boolean isVerified;
    private boolean isActive;
    private boolean canPostJobs;
    private int completionPercentage;
    private String[] missingFields;
    private String message;

    public CompanyProfileStatusResponse(boolean hasProfile, boolean isComplete,
                                        boolean isVerified, boolean isActive,
                                        int completionPercentage, String[] missingFields) {
        this.hasProfile = hasProfile;
        this.isComplete = isComplete;
        this.isVerified = isVerified;
        this.isActive = isActive;
        this.canPostJobs = isComplete && isVerified && isActive;
        this.completionPercentage = completionPercentage;
        this.missingFields = missingFields;

        if (!hasProfile) {
            this.message = "No company profile found. Please create one.";
        } else if (!isActive) {
            this.message = "Company profile is inactive. Contact admin.";
        } else if (!isComplete) {
            this.message = "Company profile is incomplete. Please complete it.";
        } else if (!isVerified) {
            this.message = "Company profile is complete but not yet verified by admin.";
        } else {
            this.message = "Company profile is complete and verified. You can post jobs!";
        }
    }
}