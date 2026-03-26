package com.jobportal.backend.dto.candidate.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.jobportal.backend.enums.ApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobApplicationResponse {
    private Long id;
    private Long jobId;
    private String jobTitle;
    private Long companyId;
    private String companyName;
    private String companyLogo;
    private ApplicationStatus status;
    private Boolean canReapply;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime appliedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime statusChangedAt;

    private String coverLetter;
    private Map<String, String> answers; // Application questions and answers
    private Integer matchScore;
    private String matchNotes;
    private Boolean isWithdrawn;
    private Boolean isFavorite;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    // Job details
    private String location;
    private String jobType;
    private Boolean isRemote;
    private String salaryRange;
    private String experienceLevel;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime applicationDeadline;

    private Boolean isApplicationOpen;
    private Boolean canWithdraw;
    private Boolean canUpdate;
}