package com.jobportal.backend.controller;

import com.jobportal.backend.dto.GenericResponse;
import com.jobportal.backend.dto.security.response.CompanyProfileResponse;
import com.jobportal.backend.service.CompanyProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
            description = "Get a company profile by ID (public access)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Profile not found")
    })
    @GetMapping("/{profileId}")
    public ResponseEntity<GenericResponse<CompanyProfileResponse>> getCompanyProfile(
            @PathVariable Long profileId) {

        log.debug("Public request for company profile ID: {}", profileId);

        CompanyProfileResponse response = companyProfileService.getProfileById(profileId);

        return ResponseEntity.ok(GenericResponse.success(response));
    }
}