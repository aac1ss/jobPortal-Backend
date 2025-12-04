package com.jobportal.backend.service.impl;

import com.jobportal.backend.entity.PasswordResetLog;
import com.jobportal.backend.entity.PasswordResetToken;
import com.jobportal.backend.entity.User;
import com.jobportal.backend.exception.TokenExpiredException;
import com.jobportal.backend.exception.TokenNotFoundException;
import com.jobportal.backend.repository.PasswordResetLogRepository;
import com.jobportal.backend.repository.PasswordResetTokenRepository;
import com.jobportal.backend.repository.UserRepository;
import com.jobportal.backend.service.EmailService;
import com.jobportal.backend.service.PasswordResetService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Service
public class PasswordResetServiceImpl implements PasswordResetService {
    private static final Logger logger = LoggerFactory.getLogger(PasswordResetServiceImpl.class);
    private static final Random random = new Random();

    @Value("${app.security.password-reset.token-expiration-ms:3600000}") // 1 hour
    private long tokenExpirationMs;

    @Value("${app.security.password-reset.otp-length:6}")
    private int otpLength;

    @Value("${app.security.password-reset.max-attempts:3}")
    private int maxOtpAttempts;

    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordResetLogRepository logRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public PasswordResetServiceImpl(PasswordResetTokenRepository tokenRepository,
                                    PasswordResetLogRepository logRepository,
                                    UserRepository userRepository,
                                    PasswordEncoder passwordEncoder,
                                    EmailService emailService) {
        this.tokenRepository = tokenRepository;
        this.logRepository = logRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    @Override
    @Transactional
    public void requestPasswordReset(String email, String ipAddress, String userAgent) {
        // Validate email format
        if (!isValidEmail(email)) {
            logger.warn("Invalid email format for password reset: {}", email);
            throw new IllegalArgumentException("Invalid email format");
        }

        User user = userRepository.findByEmail(email.toLowerCase().trim())
                .orElseThrow(() -> {
                    logger.warn("Password reset requested for non-existent email: {}", email);
                    // Don't reveal if email exists or not for security
                    return new RuntimeException("If the email exists, a reset OTP has been sent");
                });

        // FIRST, delete any existing tokens for this user
        tokenRepository.deleteByUser(user);

        // Flush immediately to ensure the delete is committed
        tokenRepository.flush();

        // Generate 6-digit OTP
        String otp = generateOTP();

        // Create new token
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(otp);
        resetToken.setUser(user);
        resetToken.setExpiryDate(LocalDateTime.now().plusSeconds(tokenExpirationMs / 1000));
        resetToken.setUsed(false);
        resetToken.setAttempts(0);

        tokenRepository.save(resetToken);

        // Log the request
        PasswordResetLog log = new PasswordResetLog();
        log.setUser(user);
        log.setRequestedAt(LocalDateTime.now());
        log.setStatus("REQUESTED");
        log.setIpAddress(ipAddress);
        log.setUserAgent(userAgent);
        logRepository.save(log);

        // Send email with OTP
        emailService.sendPasswordResetEmail(user, otp);

        logger.info("Password reset OTP created and sent for user: {}", user.getEmail());
    }

    @Override
    @Transactional
    public void resetPassword(String token, String newPassword, String ipAddress, String userAgent) {
        // Validate OTP format (6 digits)
        if (!isValidOTP(token)) {
            throw new IllegalArgumentException("Invalid OTP format");
        }

        // Find token by OTP value (not by user)
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> {
                    logger.warn("Invalid password reset OTP used: {}", token);
                    return new TokenNotFoundException("Invalid or expired OTP");
                });

        // Check if OTP is already used
        if (resetToken.isUsed()) {
            logger.warn("Attempt to use already used password reset OTP: {}", token);
            throw new TokenExpiredException("This OTP has already been used");
        }

        // Check if OTP is expired
        if (resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            logger.warn("Attempt to use expired password reset OTP: {}", token);
            throw new TokenExpiredException("OTP has expired");
        }

        // Check OTP attempts
        if (resetToken.getAttempts() >= maxOtpAttempts) {
            logger.warn("Too many failed attempts for OTP: {}", token);
            throw new TokenExpiredException("Too many failed attempts. Please request a new OTP.");
        }

        // Validate new password strength
        if (!isPasswordStrong(newPassword)) {
            resetToken.setAttempts(resetToken.getAttempts() + 1);
            tokenRepository.save(resetToken);
            throw new IllegalArgumentException("Password must be at least 8 characters with uppercase, lowercase, and number");
        }

        User user = resetToken.getUser();

        // Check if new password is same as old password
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            resetToken.setAttempts(resetToken.getAttempts() + 1);
            tokenRepository.save(resetToken);
            throw new IllegalArgumentException("New password cannot be the same as the current password");
        }

        // Update user password
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordUpdatedAt(LocalDateTime.now());
        user.unlockAccount(); // Unlock account if it was locked
        userRepository.save(user);

        resetToken.setUsed(true);
        tokenRepository.save(resetToken);


        PasswordResetLog log = logRepository.findTopByUserOrderByRequestedAtDesc(user)
                .orElseThrow(() -> new RuntimeException("No password reset request found for user"));
        log.setCompletedAt(LocalDateTime.now());
        log.setStatus("COMPLETED");
        logRepository.save(log);

        logger.info("Password successfully reset for user: {}", user.getEmail());
    }

    private String generateOTP() {
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }

    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }

    private boolean isValidOTP(String otp) {
        return otp != null && otp.matches("\\d{6}");
    }

    private boolean isPasswordStrong(String password) {
        if (password == null || password.length() < 8) return false;
        return password.matches(".*[A-Z].*") &&
                password.matches(".*[a-z].*") &&
                password.matches(".*[0-9].*");
    }

    /**
     * Verify OTP without resetting password (for frontend validation)
     */
    @Transactional
    public boolean verifyOTP(String otp) {
        if (!isValidOTP(otp)) {
            return false;
        }

        Optional<PasswordResetToken> resetToken = tokenRepository.findByToken(otp);
        if (resetToken.isEmpty()) {
            return false;
        }

        PasswordResetToken token = resetToken.get();

        if (token.isUsed() || token.getExpiryDate().isBefore(LocalDateTime.now())) {
            return false;
        }

        if (token.getAttempts() >= maxOtpAttempts) {
            return false;
        }

        return true;
    }
}