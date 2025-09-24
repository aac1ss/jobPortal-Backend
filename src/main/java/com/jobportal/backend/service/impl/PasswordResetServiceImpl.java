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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class PasswordResetServiceImpl implements PasswordResetService {
    @Value("${password.reset.token.expiration:3600000}")
    private long tokenExpirationMs;

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
    public void requestPasswordReset(String email, String ipAddress, String userAgent) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

        // Invalidate any existing tokens
        Optional<PasswordResetToken> existingToken = tokenRepository.findByUser(user);
        existingToken.ifPresent(tokenRepository::delete);

        // Create new token
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(UUID.randomUUID().toString());
        resetToken.setUser(user);
        resetToken.setExpiryDate(LocalDateTime.now().plusSeconds(tokenExpirationMs / 1000));
        resetToken.setUsed(false);
        tokenRepository.save(resetToken);

        // Log the request
        PasswordResetLog log = new PasswordResetLog();
        log.setUser(user);
        log.setRequestedAt(LocalDateTime.now());
        log.setStatus("REQUESTED");
        log.setIpAddress(ipAddress);
        log.setUserAgent(userAgent);
        logRepository.save(log);

        // Send email
        emailService.sendPasswordResetEmail(user, resetToken.getToken());
    }

    @Override
    @Transactional
    public void resetPassword(String token, String newPassword, String ipAddress, String userAgent) {
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new TokenNotFoundException("Invalid reset token"));

        if (resetToken.isUsed()) {
            throw new TokenExpiredException("Token has already been used");
        }

        if (resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new TokenExpiredException("Token has expired");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        // Mark token as used
        resetToken.setUsed(true);
        tokenRepository.save(resetToken);

        // Update log
        PasswordResetLog log = logRepository.findTopByUserOrderByRequestedAtDesc(user)
                .orElseThrow(() -> new RuntimeException("No password reset request found for user"));
        log.setCompletedAt(LocalDateTime.now());
        log.setStatus("COMPLETED");
        logRepository.save(log);
    }
}