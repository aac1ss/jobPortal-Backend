package com.jobportal.backend.repository;

import com.jobportal.backend.entity.JobDescription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobDescriptionRepository extends JpaRepository<JobDescription, Long> {
    // Simple repository, can add custom queries if needed
}