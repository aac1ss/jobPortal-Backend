package com.jobportal.backend.repository;

import com.jobportal.backend.entity.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {

    Optional<EmailVerificationToken> findByOtp(String otp);

    Optional<EmailVerificationToken> findByEmailAndUsedFalse(String email);

    @Modifying
    @Query("DELETE FROM EmailVerificationToken e WHERE e.email = :email")
    void deleteByEmail(@Param("email") String email);

    @Modifying
    @Query("DELETE FROM EmailVerificationToken e WHERE e.expiryDate < :now")
    int deleteExpiredTokens(@Param("now") LocalDateTime now);

    @Query("SELECT COUNT(e) > 0 FROM EmailVerificationToken e WHERE e.email = :email AND e.createdAt > :afterTime")
    boolean existsByEmailAndCreatedAtAfter(@Param("email") String email,
                                           @Param("afterTime") LocalDateTime afterTime);
}