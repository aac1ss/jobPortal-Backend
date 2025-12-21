package com.jobportal.backend.repository;

import com.jobportal.backend.entity.CompanyProfile;
import com.jobportal.backend.entity.Job;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobRepository extends JpaRepository<Job, Long> {

    Page<Job> findByCompany(CompanyProfile company, Pageable pageable);

    Page<Job> findByIsActive(boolean isActive, Pageable pageable);

    // Get jobs from active and verified companies only
    @Query("SELECT j FROM Job j WHERE j.isActive = true AND j.company.isActive = true AND j.company.isVerified = true")
    Page<Job> findByIsActiveAndCompanyIsActiveAndCompanyIsVerified(boolean jobActive, boolean companyActive, boolean companyVerified, Pageable pageable);

    // Add this missing method
    List<Job> findByIsActiveTrue(Pageable pageable);

    // Additional useful queries
    @Query("SELECT j FROM Job j WHERE j.isActive = true AND j.applicationDeadline > CURRENT_TIMESTAMP")
    List<Job> findOpenActiveJobs(Pageable pageable);

    @Query("SELECT j FROM Job j WHERE j.isActive = true AND j.company.id = :companyId")
    Page<Job> findByCompanyIdAndActive(@Param("companyId") Long companyId, Pageable pageable);

    @Query("SELECT COUNT(j) FROM Job j WHERE j.isActive = true")
    Long countActiveJobs();

}