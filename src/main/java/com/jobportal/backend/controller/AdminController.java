package com.jobportal.backend.controller;

import com.jobportal.backend.dto.GenericResponse;
import com.jobportal.backend.dto.admin.response.AdminDashboardResponse;
import com.jobportal.backend.security.UserPrincipal;
import com.jobportal.backend.service.AdminDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
@Tag(name = "Admin", description = "Admin management and dashboard APIs")
public class AdminController {

    private final AdminDashboardService adminDashboardService;

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get admin dashboard statistics",
            description = "Get comprehensive dashboard statistics including companies, jobs, candidates, and applications")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Dashboard statistics retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - admin access required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions")
    })
    public ResponseEntity<GenericResponse<AdminDashboardResponse>> getDashboard(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        log.info("Admin {} accessing dashboard", userPrincipal.getUsername());

        AdminDashboardResponse dashboard = adminDashboardService.getDashboardStatistics();

        return ResponseEntity.ok(GenericResponse.success(dashboard, "Dashboard statistics retrieved successfully"));
    }

    @GetMapping("/dashboard/date-range")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get admin dashboard statistics with date range",
            description = "Get dashboard statistics for a specific date range")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Dashboard statistics retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - admin access required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions")
    })
    public ResponseEntity<GenericResponse<AdminDashboardResponse>> getDashboardWithDateRange(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        log.info("Admin {} accessing dashboard with date range: {} to {}",
                userPrincipal.getUsername(), startDate, endDate);

        AdminDashboardResponse dashboard = adminDashboardService.getDashboardStatisticsWithDateRange(startDate, endDate);

        return ResponseEntity.ok(GenericResponse.success(dashboard, "Dashboard statistics retrieved successfully"));
    }

    @GetMapping("/dashboard/summary")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get quick summary statistics",
            description = "Get a quick summary of key metrics for the dashboard")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Summary statistics retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - admin access required")
    })
    public ResponseEntity<GenericResponse<AdminDashboardResponse>> getQuickSummary(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        log.debug("Admin {} accessing quick summary", userPrincipal.getUsername());

        AdminDashboardResponse summary = adminDashboardService.getDashboardStatistics();

        // Create a summary version with only key metrics
        AdminDashboardResponse quickSummary = AdminDashboardResponse.builder()
                .totalCompanies(summary.getTotalCompanies())
                .verifiedCompanies(summary.getVerifiedCompanies())
                .pendingVerification(summary.getPendingVerification())
                .activeJobs(summary.getActiveJobs())
                .totalCandidates(summary.getTotalCandidates())
                .newRegistrationsToday(summary.getNewRegistrationsToday())
                .applicationsThisWeek(summary.getApplicationsThisWeek())
                .lastUpdated(summary.getLastUpdated())
                .build();

        return ResponseEntity.ok(GenericResponse.success(quickSummary, "Quick summary retrieved successfully"));
    }
}