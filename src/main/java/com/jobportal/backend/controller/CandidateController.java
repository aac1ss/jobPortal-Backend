package com.jobportal.backend.controller;

import com.jobportal.backend.dto.GenericResponse;
import com.jobportal.backend.dto.candidate.request.*;
import com.jobportal.backend.dto.candidate.response.*;
import com.jobportal.backend.enums.ProfileVisibility;
import com.jobportal.backend.security.UserPrincipal;
import com.jobportal.backend.service.CandidateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
@RequestMapping("/api/candidate")
@Tag(name = "Candidate", description = "Candidate profile and job application APIs")
public class CandidateController {

    private final CandidateService candidateService;

    @PostMapping("/profile")
    @PreAuthorize("hasRole('CANDIDATE')")
    @Operation(summary = "Create or update candidate profile")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile saved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - not a candidate")
    })
    public ResponseEntity<GenericResponse<CandidateProfileResponse>> createOrUpdateProfile(
            @Valid @RequestBody CandidateProfileRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        log.info("Candidate {} updating profile", userPrincipal.getUsername());

        CandidateProfileResponse response = candidateService.createOrUpdateProfile(
                userPrincipal.getId(), request);

        return ResponseEntity.ok(GenericResponse.success(response, "Profile saved successfully"));
    }

    @GetMapping("/profile")
    @PreAuthorize("hasRole('CANDIDATE')")
    @Operation(summary = "Get candidate profile")
    public ResponseEntity<GenericResponse<CandidateProfileResponse>> getProfile(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        log.debug("Candidate {} getting profile", userPrincipal.getUsername());

        CandidateProfileResponse response = candidateService.getProfile(userPrincipal.getId());

        return ResponseEntity.ok(GenericResponse.success(response));
    }

    @PostMapping("/resume")
    @PreAuthorize("hasRole('CANDIDATE')")
    @Operation(summary = "Upload resume")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resume uploaded successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Profile not found")
    })
    public ResponseEntity<GenericResponse<CandidateProfileResponse>> uploadResume(
            @Valid @RequestBody ResumeUploadRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        log.info("Candidate {} uploading resume: {}", userPrincipal.getUsername(), request.getFileName());

        CandidateProfileResponse response = candidateService.uploadResume(
                userPrincipal.getId(), request);

        return ResponseEntity.ok(GenericResponse.success(response, "Resume uploaded successfully"));
    }

    @PutMapping("/profile/visibility")
    @PreAuthorize("hasRole('CANDIDATE')")
    @Operation(summary = "Update profile visibility")
    public ResponseEntity<GenericResponse<CandidateProfileResponse>> updateVisibility(
            @RequestParam ProfileVisibility visibility,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        log.info("Candidate {} updating visibility to {}",
                userPrincipal.getUsername(), visibility);

        CandidateProfileResponse response = candidateService.updateVisibility(
                userPrincipal.getId(), visibility);

        return ResponseEntity.ok(GenericResponse.success(response, "Visibility updated successfully"));
    }

    @PutMapping("/profile/status")
    @PreAuthorize("hasRole('CANDIDATE')")
    @Operation(summary = "Update active status")
    public ResponseEntity<GenericResponse<CandidateProfileResponse>> updateActiveStatus(
            @RequestParam boolean isActivelyLooking,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        log.info("Candidate {} updating active status to {}",
                userPrincipal.getUsername(), isActivelyLooking);

        CandidateProfileResponse response = candidateService.updateActiveStatus(
                userPrincipal.getId(), isActivelyLooking);

        return ResponseEntity.ok(GenericResponse.success(response, "Status updated successfully"));
    }

    @GetMapping("/profile/eligibility")
    @PreAuthorize("hasRole('CANDIDATE')")
    @Operation(summary = "Check if can apply for jobs")
    public ResponseEntity<GenericResponse<CandidateProfileResponse>> getEligibility(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        log.debug("Candidate {} checking eligibility", userPrincipal.getUsername());

        CandidateProfileResponse response = candidateService.getProfileForApplication(userPrincipal.getId());

        return ResponseEntity.ok(GenericResponse.success(response));
    }

    @DeleteMapping("/profile")
    @PreAuthorize("hasRole('CANDIDATE')")
    @Operation(summary = "Delete candidate profile")
    public ResponseEntity<GenericResponse<String>> deleteProfile(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        log.info("Candidate {} deleting profile", userPrincipal.getUsername());

        candidateService.deleteProfile(userPrincipal.getId());

        return ResponseEntity.ok(GenericResponse.success("Profile deleted successfully"));
    }

    @PostMapping("/applications")
    @PreAuthorize("hasRole('CANDIDATE')")
    @Operation(summary = "Apply for a job")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Application submitted successfully"),
            @ApiResponse(responseCode = "400", description = "Cannot apply (profile incomplete, job closed, etc.)"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "409", description = "Already applied for this job")
    })
    public ResponseEntity<GenericResponse<JobApplicationResponse>> applyForJob(
            @Valid @RequestBody JobApplicationRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        log.info("Candidate {} applying for job {}",
                userPrincipal.getUsername(), request.getJobId());

        JobApplicationResponse response = candidateService.applyForJob(
                userPrincipal.getId(), request);

        return ResponseEntity.ok(GenericResponse.success(response, "Application submitted successfully"));
    }

    @GetMapping("/applications")
    @PreAuthorize("hasRole('CANDIDATE')")
    @Operation(summary = "Get all applications")
    public ResponseEntity<GenericResponse<List<JobApplicationResponse>>> getApplications(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        log.debug("Candidate {} getting applications", userPrincipal.getUsername());

        List<JobApplicationResponse> response = candidateService.getApplications(userPrincipal.getId());

        return ResponseEntity.ok(GenericResponse.success(response));
    }

    @GetMapping("/applications/{applicationId}")
    @PreAuthorize("hasRole('CANDIDATE')")
    @Operation(summary = "Get application by ID")
    public ResponseEntity<GenericResponse<JobApplicationResponse>> getApplication(
            @PathVariable Long applicationId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        log.debug("Candidate {} getting application {}",
                userPrincipal.getUsername(), applicationId);

        JobApplicationResponse response = candidateService.getApplication(
                userPrincipal.getId(), applicationId);

        return ResponseEntity.ok(GenericResponse.success(response));
    }

    @PutMapping("/applications/{applicationId}/withdraw")
    @PreAuthorize("hasRole('CANDIDATE')")
    @Operation(summary = "Withdraw application")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Application withdrawn successfully"),
            @ApiResponse(responseCode = "400", description = "Cannot withdraw (already hired, etc.)"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Application not found")
    })
    public ResponseEntity<GenericResponse<JobApplicationResponse>> withdrawApplication(
            @PathVariable Long applicationId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        log.info("Candidate {} withdrawing application {}",
                userPrincipal.getUsername(), applicationId);

        JobApplicationResponse response = candidateService.withdrawApplication(
                userPrincipal.getId(), applicationId);

        return ResponseEntity.ok(GenericResponse.success(response, "Application withdrawn successfully"));
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('CANDIDATE')")
    @Operation(summary = "Get candidate dashboard")
    public ResponseEntity<GenericResponse<CandidateDashboardResponse>> getDashboard(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        log.debug("Candidate {} getting dashboard", userPrincipal.getUsername());

        CandidateDashboardResponse response = candidateService.getDashboard(userPrincipal.getId());

        return ResponseEntity.ok(GenericResponse.success(response));
    }

    @GetMapping("/profile/complete")
    @PreAuthorize("hasRole('CANDIDATE')")
    @Operation(summary = "Check if profile is complete")
    public ResponseEntity<GenericResponse<Boolean>> isProfileComplete(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        CandidateProfileResponse profile = candidateService.getProfile(userPrincipal.getId());

        return ResponseEntity.ok(GenericResponse.success(profile.getIsProfileComplete()));
    }

    @GetMapping("/profile/resume-uploaded")
    @PreAuthorize("hasRole('CANDIDATE')")
    @Operation(summary = "Check if resume is uploaded")
    public ResponseEntity<GenericResponse<Boolean>> isResumeUploaded(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        CandidateProfileResponse profile = candidateService.getProfile(userPrincipal.getId());

        return ResponseEntity.ok(GenericResponse.success(profile.getIsResumeUploaded()));
    }
}