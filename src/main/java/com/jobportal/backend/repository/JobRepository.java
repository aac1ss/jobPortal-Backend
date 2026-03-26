package com.jobportal.backend.repository;

import com.jobportal.backend.entity.CompanyProfile;
import com.jobportal.backend.entity.Job;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
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
    List<Job> findByCompanyRecruiterId(Long recruiterId);

    @Query("SELECT j FROM Job j WHERE j.company.recruiter.id = :recruiterId AND j.isActive = true")
    List<Job> findActiveJobsByRecruiterId(@Param("recruiterId") Long recruiterId);

    @Query("SELECT COUNT(j) FROM Job j WHERE j.company.recruiter.id = :recruiterId")
    Integer countJobsByRecruiterId(@Param("recruiterId") Long recruiterId);

    @Query("SELECT j FROM Job j WHERE j.company.recruiter.id = :recruiterId ORDER BY j.createdAt DESC")
    Page<Job> findByRecruiterId(@Param("recruiterId") Long recruiterId, Pageable pageable);

    // For search
    @Query("SELECT j FROM Job j WHERE " +
            "j.isActive = true AND " +
            "j.company.isActive = true AND " +
            "j.company.isVerified = true AND " +
            "(:keyword IS NULL OR j.title LIKE %:keyword% OR j.company.companyName LIKE %:keyword%) AND " +
            "(:location IS NULL OR j.location LIKE %:location%) AND " +
            "(:jobType IS NULL OR j.jobType = :jobType) AND " +
            "(:experienceLevel IS NULL OR j.experienceLevel = :experienceLevel) AND " +
            "(:isRemote IS NULL OR j.isRemote = :isRemote)")
    Page<Job> searchActiveJobsByKeyword(
            @Param("keyword") String keyword,
            @Param("location") String location,
            @Param("jobType") String jobType,
            @Param("experienceLevel") String experienceLevel,
            @Param("isRemote") Boolean isRemote,
            Pageable pageable);

    @Query("SELECT j FROM Job j WHERE " +
            "j.isActive = true AND " +
            "j.company.isActive = true AND " +
            "j.company.isVerified = true AND " +
            "(:location IS NULL OR j.location LIKE %:location%) AND " +
            "(:jobType IS NULL OR j.jobType = :jobType) AND " +
            "(:experienceLevel IS NULL OR j.experienceLevel = :experienceLevel) AND " +
            "(:isRemote IS NULL OR j.isRemote = :isRemote)")
    Page<Job> findActiveJobsWithFilters(
            @Param("location") String location,
            @Param("jobType") String jobType,
            @Param("experienceLevel") String experienceLevel,
            @Param("isRemote") Boolean isRemote,
            Pageable pageable);

    Page<Job> findByCompanyIdAndIsActiveAndCompanyIsActiveAndCompanyIsVerified(
            Long companyId, boolean active, boolean companyActive, boolean companyVerified, Pageable pageable);

    Page<Job> findByIsFeaturedAndIsActiveAndCompanyIsActiveAndCompanyIsVerified(
            boolean featured, boolean active, boolean companyActive, boolean companyVerified, Pageable pageable);

    @Query("SELECT j.id, COUNT(ja.id) as applicationCount " +
            "FROM Job j " +
            "LEFT JOIN JobApplication ja ON ja.job.id = j.id AND ja.appliedAt >= :sinceDate " +
            "WHERE j.isActive = true " +
            "AND j.company.isActive = true " +
            "AND j.company.isVerified = true " +
            "AND (j.applicationDeadline IS NULL OR j.applicationDeadline > CURRENT_TIMESTAMP) " +
            "GROUP BY j.id " +
            "ORDER BY applicationCount DESC")
    List<Object[]> findTrendingJobsData(@Param("sinceDate") LocalDateTime sinceDate);

    // Check if candidate has applied
    @Query("SELECT COUNT(ja) > 0 FROM JobApplication ja " +
            "WHERE ja.candidate.id = :candidateId AND ja.job.id = :jobId")
    boolean existsByCandidateIdAndJobId(@Param("candidateId") Long candidateId,
                                        @Param("jobId") Long jobId);

}

