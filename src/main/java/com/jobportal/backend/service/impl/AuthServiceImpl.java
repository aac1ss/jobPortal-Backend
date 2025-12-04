package com.jobportal.backend.service.impl;

import com.jobportal.backend.dto.security.request.LoginRequest;
import com.jobportal.backend.dto.security.request.SignupRequest;
import com.jobportal.backend.dto.security.response.LoginResponse;
import com.jobportal.backend.entity.RefreshToken;
import com.jobportal.backend.entity.User;
import com.jobportal.backend.enums.RoleEnum;
import com.jobportal.backend.exception.*;
import com.jobportal.backend.repository.UserRepository;
import com.jobportal.backend.security.JwtUtils;
import com.jobportal.backend.security.UserPrincipal;
import com.jobportal.backend.service.AuthService;
import com.jobportal.backend.service.RefreshTokenService;
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
        String username = signUpRequest.getUsername().trim();

        if (userRepository.existsByUsername(username)) {
            throw new UserAlreadyExistsException("Username already taken");
        }

        if (userRepository.existsByEmail(email)) {
            throw new UserAlreadyExistsException("Email already in use");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(encoder.encode(signUpRequest.getPassword()));
        user.setPasswordUpdatedAt(LocalDateTime.now());

        Set<RoleEnum> roles = validateAndGetRoles(signUpRequest.getRoleEnums());
        user.setRoleEnums(roles);
        user.setActive(true);

        userRepository.save(user);
        log.info("User registered: {} as {}", username, roles);
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

    private void validateRegistrationInput(SignupRequest signUpRequest) {
        if (signUpRequest.getUsername() == null || signUpRequest.getUsername().trim().length() < 3) {
            throw new ValidationException("Username must be at least 3 characters");
        }

        if (signUpRequest.getUsername().length() > 20) {
            throw new ValidationException("Username must not exceed 20 characters");
        }

        if (signUpRequest.getEmail() == null || !EMAIL_PATTERN.matcher(signUpRequest.getEmail()).matches()) {
            throw new ValidationException("Valid email required");
        }

        if (signUpRequest.getEmail().length() > 50) {
            throw new ValidationException("Email must not exceed 50 characters");
        }

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

    private Set<RoleEnum> validateAndGetRoles(Set<RoleEnum> requestedRoles) {
        if (requestedRoles == null || requestedRoles.isEmpty()) {
            Set<RoleEnum> defaultRoles = new HashSet<>();
            defaultRoles.add(RoleEnum.CANDIDATE);
            return defaultRoles;
        }
        return requestedRoles;
    }
}