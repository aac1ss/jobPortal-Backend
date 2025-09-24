package com.jobportal.backend.config;

import com.jobportal.backend.security.AuthTokenFilter;
import com.jobportal.backend.security.JwtUtils;
import com.jobportal.backend.service.impl.UserDetailsServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanConfig {
    @Bean
    public AuthTokenFilter authTokenFilter(JwtUtils jwtUtils, UserDetailsServiceImpl userDetailsService) {
        return new AuthTokenFilter(jwtUtils, userDetailsService);
    }
}