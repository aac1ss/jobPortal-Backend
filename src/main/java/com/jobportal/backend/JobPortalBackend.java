package com.jobportal.backend;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication

public class JobPortalBackend {

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    public static void main(String[] args) {
        SpringApplication.run(JobPortalBackend.class, args);
    }
    @PostConstruct
    public void logActiveDatabase() {
        if (datasourceUrl.contains("supabase")) {
            System.out.println("🔴 Connected to SUPABASE (Staging Database)");
        } else if (datasourceUrl.contains("railway")) {
            System.out.println("🔵 Connected to RAILWAY (Production Database)");
        } else {
            System.out.println("⚪ Connected to UNKNOWN Database: " + datasourceUrl);
        }
    }
}
