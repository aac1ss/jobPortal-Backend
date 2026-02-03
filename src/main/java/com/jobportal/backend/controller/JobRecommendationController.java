package com.jobportal.backend.controller;

import com.jobportal.backend.dto.GenericResponse;
import com.jobportal.backend.dto.job.response.JobRecommendationResponse;
import com.jobportal.backend.security.UserPrincipal;
import com.jobportal.backend.service.JobRecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/candidate/recommendations")
@Tag(name = "Job Recommendations", description = "APIs for personalized job recommendations")
public class JobRecommendationController {

    private final JobRecommendationService jobRecommendationService;

    @GetMapping("/personalized")
    @PreAuthorize("hasRole('CANDIDATE')")
    @Operation(summary = "Get personalized job recommendations",
            description = "Get job recommendations based on candidate's profile, skills, preferences, and behavior")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Recommendations retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Profile incomplete or invalid parameters"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Candidate profile not found")
    })
    public ResponseEntity<GenericResponse<Page<JobRecommendationResponse>>> getPersonalizedRecommendations(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Parameter(description = "Number of recommendations (default: 20, max: 100)")
            @RequestParam(defaultValue = "20") int limit,
            @Parameter(description = "Minimum match score threshold (0-100)")
            @RequestParam(defaultValue = "40") int minScore,
            @Parameter(description = "Filter by job type (FULL_TIME, PART_TIME, CONTRACT, etc.)")
            @RequestParam(required = false) String jobType,
            @Parameter(description = "Filter by location")
            @RequestParam(required = false) String location,
            @PageableDefault(size = 20) Pageable pageable) {

        log.info("Candidate {} requesting personalized job recommendations with limit: {}, minScore: {}",
                userPrincipal.getUsername(), limit, minScore);

        Page<JobRecommendationResponse> recommendations = jobRecommendationService
                .getPersonalizedRecommendations(userPrincipal.getId(), limit, minScore, jobType, location, pageable);

        return ResponseEntity.ok(GenericResponse.success(recommendations,
                "Found " + recommendations.getContent().size() + " personalized recommendations"));
    }

    @GetMapping("/trending")
    @PreAuthorize("hasRole('CANDIDATE')")
    @Operation(summary = "Get trending jobs in your field",
            description = "Get popular jobs based on application trends in your skills/industry")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trending jobs retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Candidate profile not found")
    })
    public ResponseEntity<GenericResponse<List<JobRecommendationResponse>>> getTrendingJobs(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Parameter(description = "Number of trending jobs (default: 10, max: 50)")
            @RequestParam(defaultValue = "10") int limit) {

        log.info("Candidate {} requesting trending jobs with limit: {}",
                userPrincipal.getUsername(), limit);

        List<JobRecommendationResponse> trendingJobs = jobRecommendationService
                .getTrendingJobs(userPrincipal.getId(), limit);

        return ResponseEntity.ok(GenericResponse.success(trendingJobs,
                "Found " + trendingJobs.size() + " trending jobs in your field"));
    }
}