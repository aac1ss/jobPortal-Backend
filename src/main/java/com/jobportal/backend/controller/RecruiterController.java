package com.jobportal.backend.controller;

import com.jobportal.backend.dto.GenericResponse;
import com.jobportal.backend.dto.company.request.UpdateApplicationStatusRequest;
import com.jobportal.backend.dto.company.response.JobApplicationDetailResponse;
import com.jobportal.backend.dto.company.response.JobWithApplicationsResponse;
import com.jobportal.backend.dto.company.response.RecruiterDashboardResponse;
import com.jobportal.backend.enums.ApplicationStatus;
import com.jobportal.backend.security.UserPrincipal;
import com.jobportal.backend.service.RecruiterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/recruiter")
@Tag(name = "Recruiter", description = "APIs for recruiters to manage job applications")
public class RecruiterController {

    private final RecruiterService recruiterService;

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('RECRUITER')")
    @Operation(summary = "Get recruiter dashboard statistics")
    public ResponseEntity<GenericResponse<RecruiterDashboardResponse>> getDashboard(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        log.info("Recruiter {} accessing dashboard", userPrincipal.getUsername());

        RecruiterDashboardResponse dashboard = recruiterService.getDashboard(userPrincipal.getId());

        return ResponseEntity.ok(GenericResponse.success(dashboard));
    }

    @GetMapping("/jobs/applications")
    @PreAuthorize("hasRole('RECRUITER')")
    @Operation(summary = "Get all jobs with their applications")
    public ResponseEntity<GenericResponse<List<JobWithApplicationsResponse>>> getAllJobsWithApplications(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        log.debug("Recruiter {} getting all jobs with applications", userPrincipal.getUsername());

        List<JobWithApplicationsResponse> jobs = recruiterService.getAllJobsWithApplications(userPrincipal.getId());

        return ResponseEntity.ok(GenericResponse.success(jobs));
    }

    @GetMapping("/jobs/{jobId}/applications")
    @PreAuthorize("hasRole('RECRUITER')")
    @Operation(summary = "Get applications for a specific job")
    public ResponseEntity<GenericResponse<JobWithApplicationsResponse>> getJobApplications(
            @PathVariable Long jobId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        log.debug("Recruiter {} getting applications for job {}", userPrincipal.getUsername(), jobId);

        JobWithApplicationsResponse jobWithApps = recruiterService.getJobApplications(userPrincipal.getId(), jobId);

        return ResponseEntity.ok(GenericResponse.success(jobWithApps));
    }

    @GetMapping("/applications/{applicationId}")
    @PreAuthorize("hasRole('RECRUITER')")
    @Operation(summary = "Get detailed view of an application")
    public ResponseEntity<GenericResponse<JobApplicationDetailResponse>> getApplicationDetail(
            @PathVariable Long applicationId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        log.debug("Recruiter {} viewing application {}", userPrincipal.getUsername(), applicationId);

        JobApplicationDetailResponse application = recruiterService.getApplicationDetail(
                userPrincipal.getId(), applicationId);

        return ResponseEntity.ok(GenericResponse.success(application));
    }

    @PutMapping("/applications/{applicationId}/status")
    @PreAuthorize("hasRole('RECRUITER')")
    @Operation(summary = "Update application status")
    public ResponseEntity<GenericResponse<JobApplicationDetailResponse>> updateApplicationStatus(
            @PathVariable Long applicationId,
            @Valid @RequestBody UpdateApplicationStatusRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        log.info("Recruiter {} updating application {} status to {}",
                userPrincipal.getUsername(), applicationId, request.getStatus());

        JobApplicationDetailResponse updated = recruiterService.updateApplicationStatus(
                userPrincipal.getId(), applicationId, request);

        return ResponseEntity.ok(GenericResponse.success(updated, "Application status updated successfully"));
    }

    @GetMapping("/applications/status/{status}")
    @PreAuthorize("hasRole('RECRUITER')")
    @Operation(summary = "Get applications by status")
    public ResponseEntity<GenericResponse<List<JobApplicationDetailResponse>>> getApplicationsByStatus(
            @PathVariable ApplicationStatus status,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        log.debug("Recruiter {} getting applications with status: {}",
                userPrincipal.getUsername(), status);

        List<JobApplicationDetailResponse> applications = recruiterService.getApplicationsByStatus(
                userPrincipal.getId(), status);

        return ResponseEntity.ok(GenericResponse.success(applications));
    }

    @GetMapping("/applications/search")
    @PreAuthorize("hasRole('RECRUITER')")
    @Operation(summary = "Search applications with filters")
    public ResponseEntity<GenericResponse<List<JobApplicationDetailResponse>>> searchApplications(
            @RequestParam(required = false) Long jobId,
            @RequestParam(required = false) ApplicationStatus status,
            @Parameter(description = "Search by candidate name, email, headline, cover letter, or job title")
            @RequestParam(required = false) String keyword,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        log.debug("Recruiter {} searching applications", userPrincipal.getUsername());

        List<JobApplicationDetailResponse> applications = recruiterService.searchApplications(
                userPrincipal.getId(), jobId, status, keyword);

        return ResponseEntity.ok(GenericResponse.success(applications));
    }

    @GetMapping("/jobs/{jobId}/applications/status/{status}")
    @PreAuthorize("hasRole('RECRUITER')")
    @Operation(summary = "Get applications for a job by status")
    public ResponseEntity<GenericResponse<List<JobApplicationDetailResponse>>> getJobApplicationsByStatus(
            @PathVariable Long jobId,
            @PathVariable ApplicationStatus status,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        log.debug("Recruiter {} getting applications for job {} with status: {}",
                userPrincipal.getUsername(), jobId, status);

        // First get all job applications
        JobWithApplicationsResponse jobWithApps = recruiterService.getJobApplications(
                userPrincipal.getId(), jobId);

        // Filter by status
        List<JobApplicationDetailResponse> filtered = jobWithApps.getApplications().stream()
                .filter(app -> app.getStatus() == status)
                .collect(java.util.stream.Collectors.toList());

        return ResponseEntity.ok(GenericResponse.success(filtered));
    }
}