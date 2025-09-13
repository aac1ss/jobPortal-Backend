package com.jobportal.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.jobportal.backend.config.ApiEndpointConstants.*;

@RestController
@RequestMapping(CANDIDATE_BASE)
public class CandidateController {

    @GetMapping(CANDIDATE_DASHBOARD)
    public String getCandidateDashboard() {
        return "Candidate Dashboard";
    }
}