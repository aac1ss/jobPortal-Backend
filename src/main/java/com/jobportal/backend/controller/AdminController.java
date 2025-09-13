package com.jobportal.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.jobportal.backend.config.ApiEndpointConstants.*;

@RestController
@RequestMapping(ADMIN_BASE)
public class AdminController {

    @GetMapping(ADMIN_USERS)
    public String getUsers() {
        return "Admin Users";
    }

    @GetMapping(ADMIN_COMPANIES)
    public String getCompanies() {
        return "Admin Companies";
    }
}