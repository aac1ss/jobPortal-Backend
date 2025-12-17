package com.jobportal.backend.repository;

import com.jobportal.backend.entity.Job;
import com.jobportal.backend.entity.CompanyProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobRepository extends JpaRepository<Job, Long> {

    Page<Job> findByCompany(CompanyProfile company, Pageable pageable);

    Page<Job> findByIsActive(boolean isActive, Pageable pageable);

    // Get jobs from active and verified companies only
    @org.springframework.data.jpa.repository.Query("SELECT j FROM Job j WHERE j.isActive = true AND j.company.isActive = true AND j.company.isVerified = true")
    Page<Job> findByIsActiveAndCompanyIsActiveAndCompanyIsVerified(boolean jobActive, boolean companyActive, boolean companyVerified, Pageable pageable);
}