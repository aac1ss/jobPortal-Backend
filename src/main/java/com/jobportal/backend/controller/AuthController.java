package com.jobportal.backend.controller;

import com.jobportal.backend.dto.auth.request.LoginRequest;
import com.jobportal.backend.dto.auth.request.RefreshTokenRequest;
import com.jobportal.backend.dto.auth.request.RegisterRequest;
import com.jobportal.backend.service.AuthService;
import com.jobportal.backend.security.jwt.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.jobportal.backend.config.ApiEndpointConstants.*;

@RestController
@RequestMapping(AUTH_BASE)
public class AuthController {
    private final AuthService authService;
    private final JwtUtil jwtUtil;

    public AuthController(AuthService authService, JwtUtil jwtUtil) {
        this.authService = authService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping(REGISTER_CANDIDATE)
    public ResponseEntity<?> registerCandidate(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request, "CANDIDATE"));
    }

    @PostMapping(REGISTER_RECRUITER)
    public ResponseEntity<?> registerRecruiter(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request, "RECRUITER"));
    }

    @PostMapping(LOGIN)
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping(REFRESH)
    public ResponseEntity<?> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request));
    }

    @PostMapping(LOGOUT)
    public ResponseEntity<?> logoutUser(HttpServletRequest request) {
        String token = jwtUtil.getTokenFromRequest(request);
        authService.logout(token);
        return ResponseEntity.ok().build();
    }

    @GetMapping(VERIFY_EMAIL)
    public ResponseEntity<?> verifyEmail(@RequestParam String token) {
        authService.verifyEmail(token);
        return ResponseEntity.ok().build();
    }
}