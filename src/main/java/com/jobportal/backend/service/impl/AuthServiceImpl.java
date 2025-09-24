package com.jobportal.backend.service.impl;

import com.jobportal.backend.dto.security.request.LoginRequest;
import com.jobportal.backend.dto.security.request.SignupRequest;
import com.jobportal.backend.dto.security.response.LoginResponse;
import com.jobportal.backend.entity.RefreshToken;
import com.jobportal.backend.entity.User;
import com.jobportal.backend.enums.Role;
import com.jobportal.backend.exception.AuthenticationException;
import com.jobportal.backend.exception.UserAlreadyExistsException;
import com.jobportal.backend.repository.UserRepository;
import com.jobportal.backend.security.JwtUtils;
import com.jobportal.backend.security.UserPrincipal;
import com.jobportal.backend.service.AuthService;
import com.jobportal.backend.service.RefreshTokenService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Service
public class AuthServiceImpl implements AuthService {
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
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(authentication);

            UserPrincipal userDetails = (UserPrincipal) authentication.getPrincipal();
            String jwt = jwtUtils.generateJwtToken(authentication);

            // Update user login information
            User user = userRepository.findById(userDetails.getId())
                    .orElseThrow(() -> new AuthenticationException("User not found"));
            user.recordLogin();
            userRepository.save(user);

            RefreshToken refreshToken = refreshTokenService.createRefreshToken(userDetails.getId());

            return new LoginResponse(jwt, refreshToken.getToken());
        } catch (Exception e) {
            throw new AuthenticationException("Invalid email or password");
        }
    }

    @Override
    public void registerUser(SignupRequest signUpRequest) {
        if (userRepository.existsByUsername(signUpRequest.getUsername())) {
            throw new UserAlreadyExistsException("Username is already taken!");
        }

        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            throw new UserAlreadyExistsException("Email is already in use!");
        }

        User user = new User();
        user.setUsername(signUpRequest.getUsername());
        user.setEmail(signUpRequest.getEmail());
        user.setPassword(encoder.encode(signUpRequest.getPassword()));
        user.setPasswordUpdatedAt(LocalDateTime.now());

        Set<Role> roles = new HashSet<>(signUpRequest.getRoles());
        user.setRoles(roles);
        user.setActive(true);

        userRepository.save(user);
    }
}