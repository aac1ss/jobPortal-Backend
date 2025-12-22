package com.jobportal.backend.controller;

import com.jobportal.backend.dto.GenericResponse;
import com.jobportal.backend.dto.job.request.CreateJobRequest;
import com.jobportal.backend.dto.job.request.UpdateJobRequest;
import com.jobportal.backend.dto.job.response.JobResponse;
import com.jobportal.backend.security.UserPrincipal;
import com.jobportal.backend.service.JobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/jobs")
@Tag(name = "Jobs", description = "APIs for managing job postings")
public class JobController {

    private final JobService jobService;

    @PostMapping
    @PreAuthorize("hasRole('RECRUITER')")
    @Operation(summary = "Create a new job")
    public ResponseEntity<GenericResponse<JobResponse>> createJob(
            @Valid @RequestBody CreateJobRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        log.info("Creating job for user: {}", userPrincipal.getUsername());
        JobResponse response = jobService.createJob(userPrincipal.getId(), request);
        return ResponseEntity.ok(GenericResponse.success(response, "Job created successfully"));
    }

    @GetMapping("/{jobId}")
    @Operation(summary = "Get job by ID")
    public ResponseEntity<GenericResponse<JobResponse>> getJobById(@PathVariable Long jobId) {
        log.debug("Getting job by ID: {}", jobId);
        JobResponse response = jobService.getJobById(jobId);
        return ResponseEntity.ok(GenericResponse.success(response));
    }

    @GetMapping("/my-jobs")
    @PreAuthorize("hasRole('RECRUITER')")
    @Operation(summary = "Get recruiter's jobs")
    public ResponseEntity<GenericResponse<Page<JobResponse>>> getMyJobs(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PageableDefault(size = 10) Pageable pageable) {

        log.debug("Getting jobs for recruiter: {}", userPrincipal.getUsername());
        Page<JobResponse> response = jobService.getJobsByRecruiter(userPrincipal.getId(), pageable);
        return ResponseEntity.ok(GenericResponse.success(response));
    }

    @GetMapping
    @Operation(summary = "Get all active jobs")
    public ResponseEntity<GenericResponse<Page<JobResponse>>> getAllActiveJobs(
            @PageableDefault(size = 20) Pageable pageable) {

        log.debug("Getting all active jobs");
        Page<JobResponse> response = jobService.getAllActiveJobs(pageable);
        return ResponseEntity.ok(GenericResponse.success(response));
    }

    @PutMapping("/{jobId}")
    @PreAuthorize("hasRole('RECRUITER')")
    @Operation(summary = "Update job")
    public ResponseEntity<GenericResponse<JobResponse>> updateJob(
            @PathVariable Long jobId,
            @Valid @RequestBody UpdateJobRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        log.info("Updating job ID: {}", jobId);
        JobResponse response = jobService.updateJob(jobId, request, userPrincipal.getId());
        return ResponseEntity.ok(GenericResponse.success(response, "Job updated successfully"));
    }

    @PutMapping("/{jobId}/deactivate")
    @PreAuthorize("hasRole('RECRUITER')")
    @Operation(summary = "Deactivate job")
    public ResponseEntity<GenericResponse<String>> deactivateJob(
            @PathVariable Long jobId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        log.info("Deactivating job ID: {}", jobId);
        jobService.deactivateJob(jobId, userPrincipal.getId());
        return ResponseEntity.ok(GenericResponse.success("Job deactivated successfully"));
    }

    @PutMapping("/{jobId}/activate")
    @PreAuthorize("hasRole('RECRUITER')")
    @Operation(summary = "Activate job")
    public ResponseEntity<GenericResponse<String>> activateJob(
            @PathVariable Long jobId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        log.info("Activating job ID: {}", jobId);
        jobService.activateJob(jobId, userPrincipal.getId());
        return ResponseEntity.ok(GenericResponse.success("Job activated successfully"));
    }

    @DeleteMapping("/{jobId}")
    @PreAuthorize("hasRole('RECRUITER')")
    @Operation(summary = "Delete job")
    public ResponseEntity<GenericResponse<String>> deleteJob(
            @PathVariable Long jobId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        log.info("Deleting job ID: {}", jobId);
        jobService.deleteJob(jobId, userPrincipal.getId());
        return ResponseEntity.ok(GenericResponse.success("Job deleted successfully"));
    }


}