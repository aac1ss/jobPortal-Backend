package com.jobportal.backend.service.impl;

import com.jobportal.backend.dto.security.request.LoginRequest;
import com.jobportal.backend.dto.security.request.SignupRequest;
import com.jobportal.backend.dto.security.response.LoginResponse;
import com.jobportal.backend.entity.RefreshToken;
import com.jobportal.backend.entity.User;
import com.jobportal.backend.enums.Role;
import com.jobportal.backend.exception.*;
import com.jobportal.backend.repository.UserRepository;
import com.jobportal.backend.security.JwtUtils;
import com.jobportal.backend.security.UserPrincipal;
import com.jobportal.backend.service.AuthService;
import com.jobportal.backend.service.RefreshTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class AuthServiceImpl implements AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    @Value("${app.security.max-login-attempts:5}")
    private int maxLoginAttempts;

    @Value("${app.security.account-lock-duration-minutes:30}")
    private int accountLockDurationMinutes;


    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder encoder;
    private final JwtUtils jwtUtils;
    private final RefreshTokenService refreshTokenService;

    public AuthServiceImpl(AuthenticationManager authenticationManager, UserRepository userRepository,
                           PasswordEncoder encoder, JwtUtils jwtUtils, RefreshTokenService refreshTokenService) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.encoder = encoder;
        this.jwtUtils = jwtUtils;
        this.refreshTokenService = refreshTokenService;
    }

    @Override
    @Transactional
    public LoginResponse authenticateUser(LoginRequest loginRequest) {
        String email = loginRequest.getEmail().toLowerCase().trim();

        logger.info("Authentication attempt for email: {}", email);

        // Basic validation
        if (loginRequest.getEmail() == null || loginRequest.getEmail().trim().isEmpty()) {
            throw new ValidationException("Email is required");
        }

        if (loginRequest.getPassword() == null || loginRequest.getPassword().trim().isEmpty()) {
            throw new ValidationException("Password is required");
        }

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new ValidationException("Invalid email format");
        }

        // Check if user exists
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    logger.warn("Authentication failed: User not found - {}", email);
                    return new AuthenticationException("Invalid email or password");
                });

        // Check account lock status
        if (user.isAccountLocked()) {
            logger.warn("Authentication failed: Account locked - {}", email);
            throw new AccountLockedException("Account is temporarily locked due to multiple failed attempts. Please try again in 30 minutes.");
        }

        try {
            // Authenticate user
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, loginRequest.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(authentication);

            UserPrincipal userDetails = (UserPrincipal) authentication.getPrincipal();
            String jwt = jwtUtils.generateJwtToken(authentication);

            // Update user login information
            user.recordLogin();
            userRepository.save(user);

            // Create refresh token
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(userDetails.getId());

            logger.info("User {} successfully authenticated", userDetails.getUsername());

            return new LoginResponse(
                    jwt,
                    refreshToken.getToken(),
                    userDetails.getId(),
                    userDetails.getUsername(),
                    userDetails.getEmail(),
                    userDetails.getAuthorities()
            );

        } catch (BadCredentialsException e) {
            // Record failed attempt
            user.recordFailedLogin(maxLoginAttempts, accountLockDurationMinutes);
            userRepository.save(user);

            logger.warn("Invalid credentials for email: {}", email);
            throw new AuthenticationException("Invalid email or password");
        } catch (Exception e) {
            logger.error("Authentication failed for email: {}", email, e);
            throw new AuthenticationException("Authentication failed. Please try again.");
        }
    }

    @Override
    @Transactional
    public void registerUser(SignupRequest signUpRequest) {
        logger.info("Starting user registration for email: {}", signUpRequest.getEmail());

        // Validate input
        validateRegistrationInput(signUpRequest);

        String email = signUpRequest.getEmail().toLowerCase().trim();
        String username = signUpRequest.getUsername().trim();

        // Check if username exists
        if (userRepository.existsByUsername(username)) {
            logger.warn("Registration failed: Username already taken - {}", username);
            throw new UserAlreadyExistsException("Username is already taken!");
        }

        // Check if email exists
        if (userRepository.existsByEmail(email)) {
            logger.warn("Registration failed: Email already in use - {}", email);
            throw new UserAlreadyExistsException("Email is already in use!");
        }

        try {
            // Create new user
            User user = new User();
            user.setUsername(username);
            user.setEmail(email);
            user.setPassword(encoder.encode(signUpRequest.getPassword()));
            user.setPasswordUpdatedAt(LocalDateTime.now());

            // Validate and set roles
            Set<Role> validatedRoles = validateAndGetRoles(signUpRequest.getRoles());
            user.setRoles(validatedRoles);
            user.setActive(true);

            // Save user
            User savedUser = userRepository.save(user);

            // Send welcome email (optional)
            // emailService.sendWelcomeEmail(savedUser);

            logger.info("User registered successfully: {} with roles: {}",
                    savedUser.getUsername(), savedUser.getRoles());

        } catch (Exception e) {
            logger.error("Error during user registration for email: {}", email, e);
            throw new RuntimeException("Registration failed: " + e.getMessage());
        }
    }

    private void validateRegistrationInput(SignupRequest signUpRequest) {
        if (signUpRequest.getUsername() == null || signUpRequest.getUsername().trim().length() < 3) {
            throw new ValidationException("Username must be at least 3 characters long");
        }

        if (signUpRequest.getUsername().length() > 50) {
            throw new ValidationException("Username must not exceed 50 characters");
        }

        if (signUpRequest.getEmail() == null || !EMAIL_PATTERN.matcher(signUpRequest.getEmail()).matches()) {
            throw new ValidationException("Valid email is required");
        }

        if (signUpRequest.getPassword() == null || signUpRequest.getPassword().length() < 8) {
            throw new ValidationException("Password must be at least 8 characters long");
        }

        // Basic password strength
        String password = signUpRequest.getPassword();
        if (!password.matches(".*[A-Z].*") || !password.matches(".*[a-z].*") || !password.matches(".*[0-9].*")) {
            throw new ValidationException("Password must contain at least one uppercase letter, one lowercase letter, and one number");
        }
    }

    private Set<Role> validateAndGetRoles(Set<Role> requestedRoles) {
        if (requestedRoles == null || requestedRoles.isEmpty()) {
            throw new ValidationException("At least one role is required");
        }

        Set<Role> validRoles = new HashSet<>();
        for (Role role : requestedRoles) {
            if (role != null && isValidRole(role)) {
                validRoles.add(role);
            }
        }

        if (validRoles.isEmpty()) {
            throw new ValidationException("Invalid roles provided");
        }

        return validRoles;
    }

    private boolean isValidRole(Role role) {
        return role == Role.ADMIN || role == Role.RECRUITER || role == Role.CANDIDATE;
    }
}