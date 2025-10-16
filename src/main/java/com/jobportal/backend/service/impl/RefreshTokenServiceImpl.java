package com.jobportal.backend.service.impl;

import com.jobportal.backend.entity.RefreshToken;
import com.jobportal.backend.exception.TokenExpiredException;
import com.jobportal.backend.repository.RefreshTokenRepository;
import com.jobportal.backend.repository.UserRepository;
import com.jobportal.backend.service.RefreshTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenServiceImpl implements RefreshTokenService {
    private static final Logger logger = LoggerFactory.getLogger(RefreshTokenServiceImpl.class);

    @Value("${jwt.refresh.expiration}")
    private Long refreshTokenDurationMs;

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    public RefreshTokenServiceImpl(RefreshTokenRepository refreshTokenRepository, UserRepository userRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
    }

    @Override
    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    @Override
    @Transactional
    public RefreshToken createRefreshToken(Long userId) {
        try {
            // First, delete any existing refresh token for this user
            refreshTokenRepository.deleteByUserId(userId);

            // Then create new refresh token
            RefreshToken refreshToken = new RefreshToken();
            refreshToken.setUser(userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found with id: " + userId)));
            refreshToken.setExpiryDate(Instant.now().plusMillis(refreshTokenDurationMs));
            refreshToken.setToken(UUID.randomUUID().toString());

            RefreshToken savedToken = refreshTokenRepository.save(refreshToken);
            logger.info("Created new refresh token for user ID: {}", userId);
            return savedToken;
        } catch (Exception e) {
            logger.error("Error creating refresh token for user ID: {}", userId, e);
            throw new RuntimeException("Failed to create refresh token", e);
        }
    }

    @Override
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().compareTo(Instant.now()) < 0) {
            refreshTokenRepository.delete(token);
            throw new TokenExpiredException( "Refresh token was expired. Please make a new signin request");
        }
        return token;
    }

    @Override
    @Transactional
    public void deleteByUserId(Long userId) { // Changed return type to void
        refreshTokenRepository.deleteByUserId(userId);
        logger.info("Deleted refresh token for user ID: {}", userId);
    }
}