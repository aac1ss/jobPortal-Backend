package com.jobportal.backend.service.impl;

import com.jobportal.backend.entity.Job;
import com.jobportal.backend.repository.JobRepository;
import com.jobportal.backend.service.JobService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class JobServiceImpl implements JobService {

    private final JobRepository jobRepository;

    public JobServiceImpl(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    @Override
    public List<Job> getPublicJobs() {
        // Implement logic to get public jobs
        return jobRepository.findAll();
    }

    @Override
    public List<Job> getPublicCompanies() {
        // Implement logic to get public companies
        return jobRepository.findAll();
    }
}