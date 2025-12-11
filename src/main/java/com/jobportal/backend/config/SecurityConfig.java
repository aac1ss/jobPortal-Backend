package com.jobportal.backend.config;

import com.jobportal.backend.security.AuthTokenFilter;
import com.jobportal.backend.security.JwtAuthenticationEntryPoint;
import com.jobportal.backend.service.impl.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableMethodSecurity(
        prePostEnabled = true,
        securedEnabled = true,
        jsr250Enabled = true
)
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserDetailsServiceImpl userDetailsService;
    private final AuthTokenFilter authTokenFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        authProvider.setHideUserNotFoundExceptions(false);
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(
                "http://localhost:3000",
                "http://localhost:5173",
                "http://localhost:8080",
                "https://neptalent.up.railway.app"
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Cache-Control", "X-Requested-With"));
        configuration.setExposedHeaders(List.of("Authorization", "X-Total-Count"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        // ✅ Swagger/OpenAPI documentation endpoints
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/swagger-resources/**",
                                "/swagger-resources",
                                "/webjars/**",
                                "/api-docs/**",
                                "/api-docs",
                                "/configuration/ui",
                                "/configuration/security"
                        ).permitAll()

                        // ✅ Authentication endpoints
                        .requestMatchers(
                                "/api/auth/candidate/login",
                                "/api/auth/recruiter/login",
                                "/api/auth/admin/login",
                                "/api/auth/signup",
                                "/api/auth/verify-signup",
                                "/api/auth/resend-verification",
                                "/api/auth/refresh-token",
                                "/api/auth/signout",
                                "/api/auth/password/forgot",
                                "/api/auth/password/reset",
                                "/api/auth/signin",          // backward compatibility
                                "/api/auth/refreshtoken"     // backward compatibility
                        ).permitAll()

                        // ✅ Actuator endpoints
                        .requestMatchers(
                                "/actuator/**",
                                "/actuator/health",
                                "/actuator/info"
                        ).permitAll()

                        // ✅ Static resources and error pages
                        .requestMatchers(
                                "/",
                                "/error",
                                "/favicon.ico",
                                "/api/test"
                        ).permitAll()

                        // ✅ Role-based endpoints
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/candidate/**").hasAnyRole("CANDIDATE", "ADMIN")
                        .requestMatchers("/api/recruiter/**").hasAnyRole("RECRUITER", "ADMIN")

                        // All other requests require authentication
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                );

        // Add JWT filter
        http.addFilterBefore(authTokenFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}