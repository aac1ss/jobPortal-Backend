package com.jobportal.backend.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobportal.backend.dto.security.request.VerifySignupRequest;
import com.jobportal.backend.entity.EmailVerificationToken;
import com.jobportal.backend.exception.*;
import com.jobportal.backend.repository.EmailVerificationTokenRepository;
import com.jobportal.backend.repository.UserRepository;
import com.jobportal.backend.service.EmailService;
import com.jobportal.backend.service.EmailVerificationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationServiceImpl implements EmailVerificationService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.security.email-verification.otp-length:6}")
    private int otpLength;

    @Value("${app.security.email-verification.otp-expiration-minutes:15}")
    private int otpExpirationMinutes;

    @Value("${app.security.email-verification.max-attempts:3}")
    private int maxVerificationAttempts;

    @Value("${app.security.email-verification.resend-cooldown-seconds:120}")
    private int resendCooldownSeconds;

    private final EmailVerificationTokenRepository verificationTokenRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    @Override
    @Transactional
    public void sendVerificationOtp(String email, String username, String passwordHash,
                                    String roles, HttpServletRequest request) {

        if (!isValidEmail(email)) {
            throw new ValidationException("Invalid email format");
        }

        email = email.toLowerCase().trim();

        // Check if user already exists
        if (userRepository.existsByEmail(email)) {
            throw new UserAlreadyExistsException("Email already registered");
        }

        // Check if already has pending registration
        verificationTokenRepository.findByEmailAndUsedFalse(email).ifPresent(token -> {
            if (!token.isExpired()) {
                throw new UserAlreadyExistsException("Verification already pending for this email");
            }
        });

        // Check resend cooldown
        checkResendCooldown(email);

        // Delete any existing tokens for this email
        verificationTokenRepository.deleteByEmail(email);

        // Generate OTP
        String otp = generateSecureOTP();

        // Prepare user data as JSON
        String userDataJson = createUserDataJson(username, passwordHash, roles);

        // Create verification token
        EmailVerificationToken verificationToken = new EmailVerificationToken();
        verificationToken.setOtp(otp);
        verificationToken.setEmail(email);
        verificationToken.setUserData(userDataJson);
        verificationToken.setExpiryDate(LocalDateTime.now().plusMinutes(otpExpirationMinutes));
        verificationToken.setUsed(false);
        verificationToken.setAttempts(0);
        verificationToken.setIpAddress(getClientIP(request));
        verificationToken.setUserAgent(request.getHeader("User-Agent"));

        verificationTokenRepository.save(verificationToken);

        // Send verification email
        emailService.sendVerificationEmail(email, username, otp);

        log.info("Verification OTP sent for user: {} with OTP: {}", email, otp);
    }

    @Override
    @Transactional
    public PendingRegistration verifyOtp(VerifySignupRequest verifyRequest, HttpServletRequest request) {
        validateVerificationRequest(verifyRequest);

        String email = verifyRequest.getEmail().toLowerCase().trim();
        String otp = verifyRequest.getOtp();

        // Check if user already exists
        if (userRepository.existsByEmail(email)) {
            throw new UserAlreadyExistsException("User already registered");
        }

        // Find verification token
        EmailVerificationToken verificationToken = verificationTokenRepository.findByOtp(otp)
                .orElseThrow(() -> {
                    log.warn("Invalid verification OTP: {}", otp);
                    return new TokenNotFoundException("Invalid verification code");
                });

        // Verify email matches
        if (!verificationToken.getEmail().equals(email)) {
            throw new AuthenticationException("Email does not match verification");
        }

        // Check if already used
        if (verificationToken.isUsed()) {
            throw new TokenExpiredException("Verification code already used");
        }

        // Check expiry
        if (verificationToken.isExpired()) {
            throw new TokenExpiredException("Verification code has expired");
        }

        // Check attempt limit
        if (verificationToken.getAttempts() >= maxVerificationAttempts) {
            throw new AccountLockedException("Too many verification attempts");
        }

        // Verify OTP
        if (!verificationToken.getOtp().equals(otp)) {
            verificationToken.setAttempts(verificationToken.getAttempts() + 1);
            verificationTokenRepository.save(verificationToken);
            throw new AuthenticationException("Invalid verification code");
        }

        // Mark token as used
        verificationToken.setUsed(true);
        verificationToken.setUsedAt(LocalDateTime.now());
        verificationTokenRepository.save(verificationToken);

        // Parse user data from JSON
        try {
            Map<String, String> userData = objectMapper.readValue(
                    verificationToken.getUserData(),
                    objectMapper.getTypeFactory().constructMapType(HashMap.class, String.class, String.class)
            );

            return new PendingRegistration(
                    email,
                    userData.get("username"),
                    userData.get("passwordHash"),
                    userData.get("roles")
            );

        } catch (JsonProcessingException e) {
            log.error("Failed to parse user data from token: {}", verificationToken.getId(), e);
            throw new RuntimeException("Failed to process verification data");
        }
    }

    @Override
    @Transactional
    public void resendVerificationOtp(String email, HttpServletRequest request) {
        if (!isValidEmail(email)) {
            throw new ValidationException("Invalid email format");
        }

        email = email.toLowerCase().trim();

        // Check if user already exists
        if (userRepository.existsByEmail(email)) {
            throw new UserAlreadyExistsException("User already registered");
        }

        // Find existing token
        EmailVerificationToken existingToken = verificationTokenRepository
                .findByEmailAndUsedFalse(email)
                .orElseThrow(() -> new ResourceNotFoundException("No pending registration found"));

        // Check if token is still valid
        if (!existingToken.isExpired()) {
            // Check cooldown period
            LocalDateTime nextAllowedTime = existingToken.getCreatedAt()
                    .plusSeconds(resendCooldownSeconds);

            if (LocalDateTime.now().isBefore(nextAllowedTime)) {
                long secondsRemaining = java.time.Duration.between(
                        LocalDateTime.now(), nextAllowedTime).getSeconds();
                throw new ValidationException(
                        String.format("Please wait %d seconds before requesting new code", secondsRemaining)
                );
            }
        }

        // Generate new OTP
        String newOtp = generateSecureOTP();

        // Update token
        existingToken.setOtp(newOtp);
        existingToken.setExpiryDate(LocalDateTime.now().plusMinutes(otpExpirationMinutes));
        existingToken.setAttempts(0);
        existingToken.setIpAddress(getClientIP(request));
        existingToken.setUserAgent(request.getHeader("User-Agent"));

        verificationTokenRepository.save(existingToken);

        // Parse username from userData
        try {
            Map<String, String> userData = objectMapper.readValue(
                    existingToken.getUserData(),
                    objectMapper.getTypeFactory().constructMapType(HashMap.class, String.class, String.class)
            );
            String username = userData.get("username");

            // Send new verification email
            emailService.sendVerificationEmail(email, username, newOtp);

        } catch (JsonProcessingException e) {
            log.error("Failed to parse user data for resend: {}", existingToken.getId(), e);
            throw new RuntimeException("Failed to process verification data");
        }

        log.info("Verification OTP resent for: {}", email);
    }

    @Override
    @Transactional
    public void cleanupExpiredRegistrations() {
        LocalDateTime cutoff = LocalDateTime.now();
        int deleted = verificationTokenRepository.deleteExpiredTokens(cutoff);
        log.info("Cleaned up {} expired verification tokens", deleted);
    }

    private String createUserDataJson(String username, String passwordHash, String roles) {
        Map<String, String> userData = new HashMap<>();
        userData.put("username", username);
        userData.put("passwordHash", passwordHash);
        userData.put("roles", roles);

        try {
            return objectMapper.writeValueAsString(userData);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to create user data JSON", e);
        }
    }

    private void validateVerificationRequest(VerifySignupRequest verifyRequest) {
        if (verifyRequest.getEmail() == null || !EMAIL_PATTERN.matcher(verifyRequest.getEmail()).matches()) {
            throw new ValidationException("Valid email required");
        }

        if (verifyRequest.getOtp() == null || !verifyRequest.getOtp().matches("\\d{6}")) {
            throw new ValidationException("OTP must be 6 digits");
        }
    }

    private boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    private void checkResendCooldown(String email) {
        LocalDateTime twoMinutesAgo = LocalDateTime.now().minusSeconds(resendCooldownSeconds);

        if (verificationTokenRepository.existsByEmailAndCreatedAtAfter(email, twoMinutesAgo)) {
            throw new ValidationException(
                    String.format("Please wait %d seconds before requesting new verification code", resendCooldownSeconds)
            );
        }
    }

    private String generateSecureOTP() {
        int min = (int) Math.pow(10, otpLength - 1);
        int max = (int) Math.pow(10, otpLength) - 1;
        int otpNumber = min + SECURE_RANDOM.nextInt(max - min + 1);
        return String.format("%0" + otpLength + "d", otpNumber);
    }

    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null) {
            return xfHeader.split(",")[0];
        }
        return request.getRemoteAddr();
    }
}