//package com.jobportal.backend.util;
//
//import com.jobportal.backend.service.EmailVerificationService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Component;
//
//@Slf4j
//@Component
//@RequiredArgsConstructor
//public class VerificationCleanupScheduler {
//
//    private final EmailVerificationService emailVerificationService;
//
//    // Run every hour
//    @Scheduled(cron = "0 0 * * * *")
//    public void cleanupExpiredTokens() {
//        log.info("Starting expired verification tokens cleanup...");
//        try {
//            emailVerificationService.cleanupExpiredTokens();
//            log.info("Expired verification tokens cleanup completed");
//        } catch (Exception e) {
//            log.error("Error during verification tokens cleanup", e);
//        }
//    }
//
//    // Run daily at 3 AM
//    @Scheduled(cron = "0 0 3 * * *")
//    public void cleanupUnverifiedUsers() {
//        log.info("Starting unverified users cleanup...");
//        try {
//            emailVerificationService.cleanupUnverifiedUsers();
//            log.info("Unverified users cleanup completed");
//        } catch (Exception e) {
//            log.error("Error during unverified users cleanup", e);
//        }
//    }
//}