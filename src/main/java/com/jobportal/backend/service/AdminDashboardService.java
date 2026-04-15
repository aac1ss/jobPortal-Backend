package com.jobportal.backend.service;

import com.jobportal.backend.dto.admin.response.AdminDashboardResponse;

import java.time.LocalDateTime;

public interface AdminDashboardService {
    AdminDashboardResponse getDashboardStatistics();
    AdminDashboardResponse getDashboardStatisticsWithDateRange(LocalDateTime startDate, LocalDateTime endDate);
}