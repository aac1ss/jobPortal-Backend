package com.jobportal.backend.controller;

import com.jobportal.backend.dto.GenericResponse;
import com.jobportal.backend.dto.company.request.AdminCompanyFilter;
import com.jobportal.backend.dto.company.response.CompanyProfileResponse;
import com.jobportal.backend.security.UserPrincipal;
import com.jobportal.backend.service.CompanyProfileService;
import io.swagger.v3.oas.annotations.Operation;
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

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/companies")
@Tag(name = "Company Profile - Admin", description = "Admin APIs for managing company profiles")
public class CompanyProfileAdminController {

    private final CompanyProfileService companyProfileService;

    @Operation(summary = "Get all companies for admin",
            description = "Get all companies with pagination (admin only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Companies retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - admin only")
    })
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GenericResponse<Page<CompanyProfileResponse>>> getAllCompaniesForAdmin(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {

        log.debug("Admin getting all companies");

        Page<CompanyProfileResponse> response = companyProfileService.getAllCompaniesForAdmin(pageable);

        return ResponseEntity.ok(GenericResponse.success(response));
    }


    @Operation(summary = "Get company profile by ID",
            description = "Get any company profile by ID (admin only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Profile not found")
    })
    @GetMapping("/{profileId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GenericResponse<CompanyProfileResponse>> getProfileById(
            @PathVariable Long profileId) {

        log.debug("Getting company profile by ID: {}", profileId);

        CompanyProfileResponse response = companyProfileService.getProfileById(profileId);

        return ResponseEntity.ok(GenericResponse.success(response));
    }

    @Operation(summary = "Verify company profile",
            description = "Verify a company profile (admin only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile verified successfully"),
            @ApiResponse(responseCode = "400", description = "Profile is incomplete or inactive"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - admin only"),
            @ApiResponse(responseCode = "404", description = "Profile not found")
    })
    @PostMapping("/{profileId}/verify")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GenericResponse<CompanyProfileResponse>> verifyProfile(
            @PathVariable Long profileId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        log.info("Admin {} verifying company profile ID: {}",
                userPrincipal.getUsername(), profileId);

        CompanyProfileResponse response = companyProfileService.verifyProfile(
                profileId, userPrincipal.getId());

        return ResponseEntity.ok(GenericResponse.success(response, "Company verified successfully"));
    }

    @Operation(summary = "Unverify company profile",
            description = "Remove verification from a company profile (admin only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile unverified successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - admin only"),
            @ApiResponse(responseCode = "404", description = "Profile not found")
    })
    @PostMapping("/{profileId}/unverify")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GenericResponse<CompanyProfileResponse>> unverifyProfile(
            @PathVariable Long profileId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        log.info("Admin {} unverifying company profile ID: {}",
                userPrincipal.getUsername(), profileId);

        CompanyProfileResponse response = companyProfileService.unverifyProfile(
                profileId, userPrincipal.getId());

        return ResponseEntity.ok(GenericResponse.success(response, "Company unverified successfully"));
    }

    @Operation(summary = "Activate company profile",
            description = "Activate a company profile (admin only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile activated successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - admin only"),
            @ApiResponse(responseCode = "404", description = "Profile not found")
    })
    @PostMapping("/{profileId}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GenericResponse<CompanyProfileResponse>> activateProfile(
            @PathVariable Long profileId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        log.info("Admin {} activating company profile ID: {}",
                userPrincipal.getUsername(), profileId);

        CompanyProfileResponse response = companyProfileService.activateProfile(
                profileId, userPrincipal.getId());

        return ResponseEntity.ok(GenericResponse.success(response, "Company activated successfully"));
    }

    @Operation(summary = "Deactivate company profile",
            description = "Deactivate a company profile (admin only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile deactivated successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - admin only"),
            @ApiResponse(responseCode = "404", description = "Profile not found")
    })
    @PostMapping("/{profileId}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GenericResponse<CompanyProfileResponse>> deactivateProfile(
            @PathVariable Long profileId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        log.info("Admin {} deactivating company profile ID: {}",
                userPrincipal.getUsername(), profileId);

        CompanyProfileResponse response = companyProfileService.deactivateProfile(
                profileId, userPrincipal.getId());

        return ResponseEntity.ok(GenericResponse.success(response, "Company deactivated successfully"));
    }
}