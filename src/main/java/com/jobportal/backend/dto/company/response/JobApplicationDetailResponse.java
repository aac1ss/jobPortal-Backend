package com.jobportal.backend.dto.company.response;

import com.jobportal.backend.enums.ApplicationStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class JobApplicationDetailResponse {
    private Long id;
    private Long candidateId;
    private String candidateName;
    private String candidateEmail;
    private String candidateProfilePicture;
    private String candidateHeadline;
    private Integer candidateTotalExperience;
    private ApplicationStatus status;
    private LocalDateTime appliedAt;
    private LocalDateTime statusChangedAt;
    private String coverLetter;
    private Integer matchScore;
    private String matchNotes;
    private Boolean isWithdrawn;
    private Boolean isFavorite;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
