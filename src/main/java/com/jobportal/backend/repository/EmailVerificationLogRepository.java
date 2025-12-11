package com.jobportal.backend.repository;

import com.jobportal.backend.entity.EmailVerificationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmailVerificationLogRepository extends JpaRepository<EmailVerificationLog, Long> {
}