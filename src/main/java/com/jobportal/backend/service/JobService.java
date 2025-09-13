package com.jobportal.backend.service;

import com.jobportal.backend.entity.Job;
import java.util.List;

public interface JobService {
    List<Job> getPublicJobs();
    List<Job> getPublicCompanies();
}