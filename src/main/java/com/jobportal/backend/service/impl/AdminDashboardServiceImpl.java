package com.jobportal.backend.service.impl;

import com.jobportal.backend.dto.admin.response.AdminDashboardResponse;
import com.jobportal.backend.enums.RoleEnum;
import com.jobportal.backend.repository.*;
import com.jobportal.backend.service.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private final CompanyProfileRepository companyProfileRepository;
    private final CandidateProfileRepository candidateProfileRepository;
    private final JobRepository jobRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public AdminDashboardResponse getDashboardStatistics() {
        log.info("Fetching admin dashboard statistics");

        LocalDateTime startOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime startOfWeek = LocalDateTime.of(LocalDate.now().minusDays(7), LocalTime.MIN);

        return buildDashboardResponse(startOfDay, startOfWeek);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminDashboardResponse getDashboardStatisticsWithDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Fetching admin dashboard statistics for date range: {} to {}", startDate, endDate);

        LocalDateTime startOfDay = startDate != null ? startDate : LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime startOfWeek = endDate != null ? endDate : LocalDateTime.of(LocalDate.now().minusDays(7), LocalTime.MIN);

        return buildDashboardResponse(startOfDay, startOfWeek);
    }

    private AdminDashboardResponse buildDashboardResponse(LocalDateTime startOfDay, LocalDateTime startOfWeek) {
        // Company Statistics
        Long totalCompanies = companyProfileRepository.count();
        Long verifiedCompanies = companyProfileRepository.countByIsVerifiedTrue();
        Long pendingVerification = companyProfileRepository.countByIsVerifiedFalseAndIsActiveTrue();
        Long rejectedCompanies = companyProfileRepository.countByIsVerifiedFalseAndIsActiveFalse();

        // Job Statistics
        Long activeJobs = jobRepository.countByIsActiveTrue();
        Long totalJobs = jobRepository.count();

        // Candidate Statistics
        Long totalCandidates = candidateProfileRepository.count();
        Long activeCandidates = candidateProfileRepository.countByIsActivelyLookingTrue();

        // Registration Statistics - Using fixed queries
        Long newRegistrationsToday = userRepository.countByCreatedAtAfter(startOfDay);
        Long newCandidatesToday = userRepository.countByRoleAndCreatedAtAfter(RoleEnum.CANDIDATE, startOfDay);
        Long newRecruitersToday = userRepository.countByRoleAndCreatedAtAfter(RoleEnum.RECRUITER, startOfDay);

        // Application Statistics
        Long applicationsThisWeek = jobApplicationRepository.countByAppliedAtAfter(startOfWeek);
        Long applicationsToday = jobApplicationRepository.countByAppliedAtAfter(startOfDay);
        Long totalApplications = jobApplicationRepository.count();

        // Calculate additional metrics
        Double companyVerificationRate = totalCompanies > 0 ?
                (verifiedCompanies.doubleValue() / totalCompanies.doubleValue()) * 100 : 0.0;

        Double jobFillRate = totalJobs > 0 ?
                (activeJobs.doubleValue() / totalJobs.doubleValue()) * 100 : 0.0;

        return AdminDashboardResponse.builder()
                .totalCompanies(totalCompanies)
                .verifiedCompanies(verifiedCompanies)
                .pendingVerification(pendingVerification)
                .rejectedCompanies(rejectedCompanies)
                .activeJobs(activeJobs)
                .totalJobs(totalJobs)
                .totalCandidates(totalCandidates)
                .activeCandidates(activeCandidates)
                .newRegistrationsToday(newRegistrationsToday)
                .newCandidatesToday(newCandidatesToday)
                .newRecruitersToday(newRecruitersToday)
                .applicationsThisWeek(applicationsThisWeek)
                .applicationsToday(applicationsToday)
                .totalApplications(totalApplications)
                .companyVerificationRate(Math.round(companyVerificationRate * 100.0) / 100.0)
                .jobFillRate(Math.round(jobFillRate * 100.0) / 100.0)
                .lastUpdated(LocalDateTime.now())
                .build();
    }
}