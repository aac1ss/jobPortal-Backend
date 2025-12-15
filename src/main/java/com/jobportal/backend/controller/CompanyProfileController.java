package com.jobportal.backend.controller;

import com.jobportal.backend.dto.GenericResponse;
import com.jobportal.backend.dto.security.request.CompanyProfileRequest;
import com.jobportal.backend.dto.security.response.CompanyProfileResponse;
import com.jobportal.backend.dto.security.response.CompanyProfileStatusResponse;
import com.jobportal.backend.security.UserPrincipal;
import com.jobportal.backend.service.CompanyProfileService;
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

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/recruiter/profile")
@Tag(name = "Company Profile", description = "APIs for managing company profiles")
public class CompanyProfileController {

    private final CompanyProfileService companyProfileService;

    @Operation(summary = "Create company profile",
            description = "Create a new company profile for the recruiter")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input or profile already exists"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - only recruiters can create profiles"),
            @ApiResponse(responseCode = "404", description = "Recruiter not found")
    })
    @PostMapping
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<GenericResponse<CompanyProfileResponse>> createProfile(
            @Valid @RequestBody CompanyProfileRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        log.info("Creating company profile for user: {}", userPrincipal.getUsername());

        CompanyProfileResponse response = companyProfileService.createProfile(
                userPrincipal.getId(), request);

        return ResponseEntity.ok(GenericResponse.success(response, "Company profile created successfully"));
    }

    @Operation(summary = "Update company profile",
            description = "Update the recruiter's company profile")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Profile not found")
    })
    @PutMapping
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<GenericResponse<CompanyProfileResponse>> updateProfile(
            @Valid @RequestBody CompanyProfileRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        log.info("Updating company profile for user: {}", userPrincipal.getUsername());

        CompanyProfileResponse response = companyProfileService.updateProfile(
                userPrincipal.getId(), request);

        return ResponseEntity.ok(GenericResponse.success(response, "Company profile updated successfully"));
    }

    @Operation(summary = "Get company profile",
            description = "Get the recruiter's company profile")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Profile not found")
    })
    @GetMapping
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<GenericResponse<CompanyProfileResponse>> getProfile(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        log.debug("Getting company profile for user: {}", userPrincipal.getUsername());

        CompanyProfileResponse response = companyProfileService.getProfile(userPrincipal.getId());

        return ResponseEntity.ok(GenericResponse.success(response));
    }

    @Operation(summary = "Get profile status",
            description = "Get completion status of the company profile")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Status retrieved successfully")
    })
    @GetMapping("/status")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<GenericResponse<CompanyProfileStatusResponse>> getProfileStatus(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        log.debug("Getting profile status for user: {}", userPrincipal.getUsername());

        CompanyProfileStatusResponse response = companyProfileService.getProfileStatus(
                userPrincipal.getId());

        return ResponseEntity.ok(GenericResponse.success(response));
    }

    @Operation(summary = "Delete company profile",
            description = "Delete (soft delete) the recruiter's company profile")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Profile not found")
    })
    @DeleteMapping
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<GenericResponse<String>> deleteProfile(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        log.info("Deleting company profile for user: {}", userPrincipal.getUsername());

        companyProfileService.deleteProfile(userPrincipal.getId());

        return ResponseEntity.ok(GenericResponse.success("Company profile deleted successfully"));
    }

    @Operation(summary = "Check if profile is complete",
            description = "Check if the recruiter's company profile is complete")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Check completed")
    })
    @GetMapping("/complete")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<GenericResponse<Boolean>> isProfileComplete(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        log.debug("Checking profile completion for user: {}", userPrincipal.getUsername());

        boolean isComplete = companyProfileService.isProfileComplete(userPrincipal.getId());

        return ResponseEntity.ok(GenericResponse.success(isComplete));
    }

    @Operation(summary = "Check if can post jobs",
            description = "Check if the recruiter can post jobs (profile complete and active)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Check completed")
    })
    @GetMapping("/can-post-jobs")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<GenericResponse<Boolean>> canPostJobs(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        log.debug("Checking job posting eligibility for user: {}", userPrincipal.getUsername());

        boolean canPostJobs = companyProfileService.canPostJobs(userPrincipal.getId());

        return ResponseEntity.ok(GenericResponse.success(canPostJobs));
    }
}