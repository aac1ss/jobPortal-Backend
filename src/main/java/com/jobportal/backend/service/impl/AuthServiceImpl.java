package com.jobportal.backend.service.impl;

import com.jobportal.backend.dto.security.request.LoginRequest;
import com.jobportal.backend.dto.security.request.SignupRequest;
import com.jobportal.backend.dto.security.request.VerifySignupRequest;
import com.jobportal.backend.dto.security.response.LoginResponse;
import com.jobportal.backend.entity.RefreshToken;
import com.jobportal.backend.entity.User;
import com.jobportal.backend.enums.RoleEnum;
import com.jobportal.backend.exception.AccountLockedException;
import com.jobportal.backend.exception.AuthenticationException;
import com.jobportal.backend.exception.UserAlreadyExistsException;
import com.jobportal.backend.exception.ValidationException;
import com.jobportal.backend.repository.UserRepository;
import com.jobportal.backend.security.JwtUtils;
import com.jobportal.backend.security.UserPrincipal;
import com.jobportal.backend.service.AuthService;
import com.jobportal.backend.service.EmailVerificationService;
import com.jobportal.backend.service.RefreshTokenService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");

    @Value("${jwt.expiration:900000}")
    private long jwtExpirationMs;

    @Value("${app.security.max-login-attempts:5}")
    private int maxLoginAttempts;

    @Value("${app.security.account-lock-duration-minutes:30}")
    private int accountLockDurationMinutes;

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder encoder;
    private final JwtUtils jwtUtils;
    private final RefreshTokenService refreshTokenService;
    private final EmailVerificationService emailVerificationService;

    @Override
    @Transactional
    public LoginResponse authenticateCandidate(LoginRequest loginRequest) {
        return authenticateUserWithRole(loginRequest, RoleEnum.CANDIDATE);
    }

    @Override
    @Transactional
    public LoginResponse authenticateRecruiter(LoginRequest loginRequest) {
        return authenticateUserWithRole(loginRequest, RoleEnum.RECRUITER);
    }

    @Override
    @Transactional
    public LoginResponse authenticateAdmin(LoginRequest loginRequest) {
        return authenticateUserWithRole(loginRequest, RoleEnum.ADMIN);
    }

    private LoginResponse authenticateUserWithRole(LoginRequest loginRequest, RoleEnum requiredRole) {
        String email = loginRequest.getEmail().toLowerCase().trim();

        validateLoginInput(loginRequest);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("User not found: {}", email);
                    return new AuthenticationException("Invalid credentials");
                });
        if (!user.isEmailVerified()) {
            log.warn("Login attempt with unverified email: {}", email);
            throw new AuthenticationException("Please verify your email before logging in");
        }

        if (user.isAccountLocked()) {
            log.warn("Account locked: {}", email);
            throw new AccountLockedException("Account is temporarily locked");
        }

        if (!user.hasRole(requiredRole)) {
            log.warn("Role mismatch: {} expected {}, user has {}",
                    email, requiredRole, user.getRoleEnums());
            user.recordFailedLogin(maxLoginAttempts, accountLockDurationMinutes);
            userRepository.save(user);
            throw new AuthenticationException("Invalid credentials");
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, loginRequest.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(authentication);

            UserPrincipal userDetails = (UserPrincipal) authentication.getPrincipal();
            String jwt = jwtUtils.generateJwtToken(authentication);

            user.recordLogin();
            userRepository.save(user);

            RefreshToken refreshToken = refreshTokenService.createRefreshToken(userDetails.getId());

            log.info("{} logged in: {}", requiredRole, userDetails.getUsername());

            return new LoginResponse(
                    jwt,
                    refreshToken.getToken(),
                    jwtExpirationMs / 1000
            );

        } catch (BadCredentialsException e) {
            user.recordFailedLogin(maxLoginAttempts, accountLockDurationMinutes);
            userRepository.save(user);

            log.warn("Invalid credentials: {}", email);
            throw new AuthenticationException("Invalid credentials");
        } catch (Exception e) {
            log.error("Authentication failed: {}", email, e);
            throw new AuthenticationException("Authentication failed");
        }
    }

    @Override
    @Transactional
    public void registerUser(SignupRequest signUpRequest) {
        validateRegistrationInput(signUpRequest);

        String email = signUpRequest.getEmail().toLowerCase().trim();
        String fullName = signUpRequest.getFullName().trim();

        if (userRepository.existsByEmail(email)) {
            throw new UserAlreadyExistsException("Email already in use");
        }

        // Hash password
        String passwordHash = encoder.encode(signUpRequest.getPassword());

        // Convert roles to JSON string
        String rolesJson = convertRolesToJson(signUpRequest.getRoleEnums());

        // Get HTTP request for IP tracking
        HttpServletRequest request = getCurrentHttpRequest();

        // Send verification OTP (does NOT save user yet)
        emailVerificationService.sendVerificationOtp(email, fullName, passwordHash, rolesJson, request);

        log.info("Signup initiated for user: {}. Verification OTP sent.", email);
    }

    @Override
    @Transactional
    public User verifyAndCompleteSignup(VerifySignupRequest verifyRequest) {
        HttpServletRequest request = getCurrentHttpRequest();

        EmailVerificationService.PendingRegistration pending =
                emailVerificationService.verifyOtp(verifyRequest, request);

        User user = createUserFromPendingRegistration(pending);
        User savedUser = userRepository.save(user);

        log.info("User registration completed: {} - {}", savedUser.getEmail(), savedUser.getFullName());

        return savedUser;
    }

    @Transactional
    public void resendVerificationOtp(String email) {
        // Get HTTP request for IP tracking
        HttpServletRequest request = getCurrentHttpRequest();

        emailVerificationService.resendVerificationOtp(email, request);

        log.info("Verification OTP resent for: {}", email);
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            throw new ValidationException("Refresh token is required");
        }

        // Delete the refresh token
        refreshTokenService.deleteByToken(refreshToken);

        log.info("User logged out successfully. Refresh token invalidated.");
    }

    private User createUserFromPendingRegistration(EmailVerificationService.PendingRegistration pending) {
        User user = new User();
        user.setFullName(pending.getFullName());
        user.setEmail(pending.getEmail());
        user.setPassword(pending.getPasswordHash());
        user.setPasswordUpdatedAt(LocalDateTime.now());

        // Parse roles from JSON
        Set<RoleEnum> roles = parseRolesFromJson(pending.getRoles());
        user.setRoleEnums(roles);

        user.setActive(true);
        user.setEmailVerified(true);
        user.setEmailVerifiedAt(LocalDateTime.now());

        return user;
    }

    private String convertRolesToJson(Set<RoleEnum> roles) {
        Set<RoleEnum> validRoles = validateAndGetRoles(roles);
        // Simple comma-separated string for roles
        return String.join(",", validRoles.stream().map(Enum::name).toArray(String[]::new));
    }

    private Set<RoleEnum> parseRolesFromJson(String rolesJson) {
        Set<RoleEnum> roles = new HashSet<>();
        if (rolesJson != null && !rolesJson.isEmpty()) {
            for (String roleName : rolesJson.split(",")) {
                try {
                    roles.add(RoleEnum.valueOf(roleName.trim()));
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid role in pending registration: {}", roleName);
                }
            }
        }
        // Default to CANDIDATE if no roles
        if (roles.isEmpty()) {
            roles.add(RoleEnum.CANDIDATE);
        }
        return roles;
    }

    private HttpServletRequest getCurrentHttpRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes)
                RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            return attributes.getRequest();
        }
        throw new IllegalStateException("No HTTP request available");
    }

    private void validateRegistrationInput(SignupRequest signUpRequest) {
        // Full name validation - at least 2 words
        String fullName = signUpRequest.getFullName();
        if (fullName == null || fullName.trim().isEmpty()) {
            throw new ValidationException("Full name is required");
        }

        fullName = fullName.trim();

        // Check for minimum 2 characters
        if (fullName.length() < 2) {
            throw new ValidationException("Full name must be at least 2 characters");
        }

        // Check for maximum 100 characters
        if (fullName.length() > 100) {
            throw new ValidationException("Full name must not exceed 100 characters");
        }

        // Check for at least 2 words (separated by space)
        String[] nameParts = fullName.split("\\s+");
        if (nameParts.length < 2) {
            throw new ValidationException("Full name must contain at least 2 words (e.g., 'John Smith')");
        }

        // Check each name part for minimum length
        for (String part : nameParts) {
            if (part.length() < 1) {
                throw new ValidationException("Each name part must have at least 1 character");
            }
        }

        // Email validation
        if (signUpRequest.getEmail() == null || !EMAIL_PATTERN.matcher(signUpRequest.getEmail()).matches()) {
            throw new ValidationException("Valid email required");
        }

        if (signUpRequest.getEmail().length() > 50) {
            throw new ValidationException("Email must not exceed 50 characters");
        }

        // Password validation
        if (signUpRequest.getPassword() == null || signUpRequest.getPassword().length() < 6) {
            throw new ValidationException("Password must be at least 6 characters");
        }

        if (signUpRequest.getPassword().length() > 40) {
            throw new ValidationException("Password must not exceed 40 characters");
        }

        String password = signUpRequest.getPassword();
        if (!password.matches(".*[A-Z].*") || !password.matches(".*[a-z].*") || !password.matches(".*[0-9].*")) {
            throw new ValidationException("Password must contain uppercase, lowercase and number");
        }
    }


    private void validateLoginInput(LoginRequest loginRequest) {
        if (loginRequest.getEmail() == null || loginRequest.getEmail().trim().isEmpty()) {
            throw new ValidationException("Email is required");
        }

        if (loginRequest.getPassword() == null || loginRequest.getPassword().trim().isEmpty()) {
            throw new ValidationException("Password is required");
        }

        if (!EMAIL_PATTERN.matcher(loginRequest.getEmail().toLowerCase().trim()).matches()) {
            throw new ValidationException("Invalid email format");
        }
    }

    private Set<RoleEnum> validateAndGetRoles(Set<RoleEnum> requestedRoles) {
        if (requestedRoles == null || requestedRoles.isEmpty()) {
            Set<RoleEnum> defaultRoles = new HashSet<>();
            defaultRoles.add(RoleEnum.CANDIDATE);
            return defaultRoles;
        }
        return requestedRoles;
    }
}