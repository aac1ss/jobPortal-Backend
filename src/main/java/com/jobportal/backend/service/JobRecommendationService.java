package com.jobportal.backend.service;

import com.jobportal.backend.dto.RecommendationScore;
import com.jobportal.backend.dto.job.response.JobRecommendationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface JobRecommendationService {

    /**
     * Get personalized job recommendations based on candidate profile
     */
    Page<JobRecommendationResponse> getPersonalizedRecommendations(
            Long candidateId, int limit, int minScore,
            String jobType, String location, Pageable pageable);

    /**
     * Get trending jobs in candidate's field
     */
    List<JobRecommendationResponse> getTrendingJobs(Long candidateId, int limit);

    /**
     * Calculate comprehensive recommendation score
     */
    RecommendationScore calculateRecommendationScore(
            Long candidateId, Long jobId);

    /**
     * Get reasons why a job was recommended
     */
    List<String> getRecommendationReasons(Long candidateId, Long jobId);
}
