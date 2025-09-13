package com.jobportal.backend.config;

public class ApiEndpointConstants {

    // Base endpoints
    public static final String API_BASE = "/api";
    public static final String AUTH_BASE = API_BASE + "/auth";
    public static final String PUBLIC_BASE = API_BASE + "/public";
    public static final String CANDIDATE_BASE = API_BASE + "/candidate";
    public static final String RECRUITER_BASE = API_BASE + "/recruiter";
    public static final String ADMIN_BASE = API_BASE + "/admin";

    // Auth endpoints
    public static final String REGISTER_CANDIDATE = "/register/candidate";
    public static final String REGISTER_RECRUITER = "/register/recruiter";
    public static final String LOGIN = "/login";
    public static final String REFRESH = "/refresh";
    public static final String LOGOUT = "/logout";
    public static final String VERIFY_EMAIL = "/verify-email";

    // Public endpoints
    public static final String PUBLIC_JOBS = "/jobs";
    public static final String PUBLIC_COMPANIES = "/companies";

    // Candidate endpoints
    public static final String CANDIDATE_DASHBOARD = "/dashboard";

    // Recruiter endpoints
    public static final String RECRUITER_DASHBOARD = "/dashboard";
    public static final String RECRUITER_COMPANIES = "/companies";
    public static final String RECRUITER_JOBS = "/jobs";

    // Admin endpoints
    public static final String ADMIN_USERS = "/users";
    public static final String ADMIN_COMPANIES = "/companies";

    // Admin login (hidden URL)
    public static final String ADMIN_LOGIN = "/admin/login";

    private ApiEndpointConstants() {
        // Utility class, prevent instantiation
    }
}