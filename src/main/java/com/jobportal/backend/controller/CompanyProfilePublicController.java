package com.jobportal.backend.controller;

import com.jobportal.backend.dto.GenericResponse;
import com.jobportal.backend.dto.company.response.CompanyProfileResponse;
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
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/companies")
@Tag(name = "Companies - Public", description = "Public APIs for viewing company profiles")
public class CompanyProfilePublicController {

    private final CompanyProfileService companyProfileService;

    @Operation(summary = "Get company profile",
            description = "Get a company profile by ID (public access - only verified companies)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Profile not found or not verified")
    })
    @GetMapping("/{profileId}")
    public ResponseEntity<GenericResponse<CompanyProfileResponse>> getCompanyProfile(
            @PathVariable Long profileId) {

        log.debug("Public request for company profile ID: {}", profileId);

        // FIX: Use getPublicProfileById() instead of getProfileById()
        CompanyProfileResponse response = companyProfileService.getPublicProfileById(profileId);

        return ResponseEntity.ok(GenericResponse.success(response));
    }

    @Operation(summary = "Get all verified companies",
            description = "Get all verified and active companies (paginated)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Companies retrieved successfully")
    })
    @GetMapping
    public ResponseEntity<GenericResponse<Page<CompanyProfileResponse>>> getAllVerifiedCompanies(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {

        log.debug("Getting all verified companies");

        Page<CompanyProfileResponse> response = companyProfileService.getAllVerifiedCompanies(pageable);

        return ResponseEntity.ok(GenericResponse.success(response));
    }
}