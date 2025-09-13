package com.jobportal.backend.controller;

import com.jobportal.backend.service.CompanyService;
import com.jobportal.backend.service.JobService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.jobportal.backend.config.ApiEndpointConstants.*;

@RestController
@RequestMapping(PUBLIC_BASE)
public class PublicController {

    private final JobService jobService;
    private final CompanyService companyService;

    public PublicController(JobService jobService, CompanyService companyService) {
        this.jobService = jobService;
        this.companyService = companyService;
    }

    @GetMapping(PUBLIC_JOBS)
    public ResponseEntity<?> getPublicJobs() {
        return ResponseEntity.ok(jobService.getPublicJobs());
    }

    @GetMapping(PUBLIC_COMPANIES)
    public ResponseEntity<?> getPublicCompanies() {
        return ResponseEntity.ok(companyService.getPublicCompanies());
    }
}