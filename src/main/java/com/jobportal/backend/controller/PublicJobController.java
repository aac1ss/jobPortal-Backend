package com.jobportal.backend.controller;

import com.jobportal.backend.dto.GenericResponse;
import com.jobportal.backend.dto.job.response.JobResponse;
import com.jobportal.backend.service.JobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/public/jobs")
@Tag(name = "Public Jobs", description = "Public APIs for browsing job postings (No authentication required)")
public class PublicJobController {

    private final JobService jobService;

    @GetMapping
    @Operation(summary = "Get all active jobs (Public)")
    public ResponseEntity<GenericResponse<Page<JobResponse>>> getAllActiveJobs(
            @PageableDefault(size = 20) Pageable pageable) {

        log.debug("Public API: Getting all active jobs");
        Page<JobResponse> response = jobService.getAllActiveJobs(pageable);
        return ResponseEntity.ok(GenericResponse.success(response));
    }

    @GetMapping("/{jobId}")
    @Operation(summary = "Get job details by ID (Public)")
    public ResponseEntity<GenericResponse<JobResponse>> getJobById(@PathVariable Long jobId) {
        log.debug("Public API: Getting job by ID: {}", jobId);
        JobResponse response = jobService.getJobById(jobId);
        return ResponseEntity.ok(GenericResponse.success(response));
    }

    @GetMapping("/search")
    @Operation(summary = "Search jobs with filters (Public)")
    public ResponseEntity<GenericResponse<Page<JobResponse>>> searchJobs(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String jobType,
            @RequestParam(required = false) String experienceLevel,
            @RequestParam(required = false) Boolean isRemote,
            @PageableDefault(size = 20) Pageable pageable) {

        log.debug("Public API: Searching jobs with filters - keyword: {}, location: {}, jobType: {}, experienceLevel: {}, remote: {}",
                keyword, location, jobType, experienceLevel, isRemote);

        // You'll need to implement this method in JobService
        Page<JobResponse> response = jobService.searchActiveJobs(keyword, location, jobType, experienceLevel, isRemote, pageable);
        return ResponseEntity.ok(GenericResponse.success(response));
    }

    @GetMapping("/company/{companyId}")
    @Operation(summary = "Get active jobs by company ID (Public)")
    public ResponseEntity<GenericResponse<Page<JobResponse>>> getActiveJobsByCompany(
            @PathVariable Long companyId,
            @PageableDefault(size = 10) Pageable pageable) {

        log.debug("Public API: Getting active jobs for company ID: {}", companyId);
        Page<JobResponse> response = jobService.getActiveJobsByCompany(companyId, pageable);
        return ResponseEntity.ok(GenericResponse.success(response));
    }

    @GetMapping("/featured")
    @Operation(summary = "Get featured jobs (Public)")
    public ResponseEntity<GenericResponse<Page<JobResponse>>> getFeaturedJobs(
            @PageableDefault(size = 10) Pageable pageable) {

        log.debug("Public API: Getting featured jobs");
        Page<JobResponse> response = jobService.getFeaturedJobs(pageable);
        return ResponseEntity.ok(GenericResponse.success(response));
    }
}