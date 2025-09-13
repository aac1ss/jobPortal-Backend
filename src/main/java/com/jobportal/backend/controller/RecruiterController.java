package com.jobportal.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.jobportal.backend.config.ApiEndpointConstants.*;

@RestController
@RequestMapping(RECRUITER_BASE)
public class RecruiterController {

    @GetMapping(RECRUITER_DASHBOARD)
    public String getRecruiterDashboard() {
        return "Recruiter Dashboard";
    }
}