package com.jobportal.backend.dto.candidate.response;

import com.jobportal.backend.dto.ProfileHealth;
import com.jobportal.backend.dto.candidate.RecentApplication;
import com.jobportal.backend.dto.candidate.RecommendedJob;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CandidateDashboardResponse {
    private CandidateProfileResponse profile;

    private Integer totalApplications;
    private Integer pendingApplications;
    private Integer interviewApplications;
    private Integer rejectedApplications;
    private Integer hiredApplications;

    private List<RecentApplication> recentApplications;
    private ProfileHealth profileHealth;
    private List<RecommendedJob> recommendedJobs;

}
