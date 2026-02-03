package com.jobportal.backend.repository;

import com.jobportal.backend.entity.JobApplication;
import com.jobportal.backend.enums.ApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {

    Optional<JobApplication> findByJobIdAndCandidateId(Long jobId, Long candidateId);

    Optional<JobApplication> findByJobIdAndCandidateIdAndIsWithdrawnFalse(Long jobId, Long candidateId);

    List<JobApplication> findByCandidateId(Long candidateId);

    List<JobApplication> findByCandidateIdAndIsWithdrawnFalse(Long candidateId);

    Page<JobApplication> findByCandidateId(Long candidateId, Pageable pageable);

    Page<JobApplication> findByCandidateIdAndIsWithdrawnFalse(Long candidateId, Pageable pageable);

    List<JobApplication> findByJobId(Long jobId);

    List<JobApplication> findByJobIdAndIsWithdrawnFalse(Long jobId);

    @Query("SELECT COUNT(ja) FROM JobApplication ja WHERE ja.candidate.id = :candidateId AND ja.isWithdrawn = false")
    Integer countByCandidateId(@Param("candidateId") Long candidateId);

    @Query("SELECT COUNT(ja) FROM JobApplication ja WHERE ja.candidate.id = :candidateId " +
            "AND ja.status = :status AND ja.isWithdrawn = false")
    Integer countByCandidateIdAndStatus(@Param("candidateId") Long candidateId,
                                        @Param("status") ApplicationStatus status);

    @Query("SELECT CASE WHEN COUNT(ja) > 0 THEN true ELSE false END FROM JobApplication ja " +
            "WHERE ja.job.id = :jobId AND ja.candidate.id = :candidateId AND ja.isWithdrawn = false")
    boolean existsByJobIdAndCandidateId(@Param("jobId") Long jobId, @Param("candidateId") Long candidateId);

    @Query("SELECT ja FROM JobApplication ja WHERE ja.candidate.id = :candidateId " +
            "AND ja.isWithdrawn = false ORDER BY ja.appliedAt DESC")
    List<JobApplication> findRecentByCandidateId(@Param("candidateId") Long candidateId, Pageable pageable);

    @Query("SELECT ja FROM JobApplication ja WHERE ja.candidate.user.id = :userId AND ja.isWithdrawn = false")
    List<JobApplication> findByUserId(@Param("userId") Long userId);

    @Query("SELECT ja FROM JobApplication ja WHERE ja.candidate.user.id = :userId " +
            "AND ja.isWithdrawn = false ORDER BY ja.appliedAt DESC")
    List<JobApplication> findByUserIdOrderByAppliedAtDesc(@Param("userId") Long userId);

    @Query("SELECT ja FROM JobApplication ja WHERE ja.candidate.user.id = :userId AND ja.job.id = :jobId " +
            "AND ja.isWithdrawn = false")
    Optional<JobApplication> findByUserIdAndJobId(@Param("userId") Long userId, @Param("jobId") Long jobId);

    @Query("SELECT ja FROM JobApplication ja WHERE ja.candidate.user.id = :userId AND ja.status = :status " +
            "AND ja.isWithdrawn = false")
    List<JobApplication> findByUserIdAndStatus(@Param("userId") Long userId,
                                               @Param("status") ApplicationStatus status);

    @Query("SELECT ja FROM JobApplication ja WHERE ja.candidate.id = :candidateId " +
            "AND ja.job.id = :jobId AND ja.isWithdrawn = false")
    Optional<JobApplication> findByCandidateIdAndJobId(@Param("candidateId") Long candidateId,
                                                       @Param("jobId") Long jobId);

    boolean existsByJobIdAndCandidateIdAndIsWithdrawnFalse(Long jobId, Long candidateId);

    @Query("SELECT ja FROM JobApplication ja " +
            "WHERE ja.job.company.recruiter.id = :recruiterId " +
            "AND ja.job.id = :jobId " +
            "ORDER BY ja.appliedAt DESC")
    List<JobApplication> findByRecruiterIdAndJobId(@Param("recruiterId") Long recruiterId,
                                                   @Param("jobId") Long jobId);

    @Query("SELECT ja FROM JobApplication ja " +
            "WHERE ja.job.company.recruiter.id = :recruiterId " +
            "AND ja.isWithdrawn = false " +
            "ORDER BY ja.appliedAt DESC")
    List<JobApplication> findByRecruiterId(@Param("recruiterId") Long recruiterId);

    @Query("SELECT COUNT(ja) FROM JobApplication ja " +
            "WHERE ja.job.company.recruiter.id = :recruiterId " +
            "AND ja.isWithdrawn = false")
    Integer countByRecruiterId(@Param("recruiterId") Long recruiterId);

    @Query("SELECT COUNT(ja) FROM JobApplication ja " +
            "WHERE ja.job.company.recruiter.id = :recruiterId " +
            "AND ja.status = :status " +
            "AND ja.isWithdrawn = false")
    Integer countByRecruiterIdAndStatus(@Param("recruiterId") Long recruiterId,
                                        @Param("status") ApplicationStatus status);

    @Query("SELECT COUNT(ja) FROM JobApplication ja " +
            "WHERE ja.job.company.recruiter.id = :recruiterId " +
            "AND ja.isWithdrawn = false " +
            "AND MONTH(ja.appliedAt) = MONTH(CURRENT_DATE) " +
            "AND YEAR(ja.appliedAt) = YEAR(CURRENT_DATE)")
    Integer countApplicationsThisMonth(@Param("recruiterId") Long recruiterId);

    @Query("SELECT ja FROM JobApplication ja " +
            "WHERE ja.job.company.recruiter.id = :recruiterId " +
            "AND ja.status = :status " +
            "AND ja.isWithdrawn = false " +
            "ORDER BY ja.appliedAt DESC")
    List<JobApplication> findByRecruiterIdAndStatus(@Param("recruiterId") Long recruiterId,
                                                    @Param("status") ApplicationStatus status);

    @Query("SELECT COUNT(DISTINCT ja.job.id) FROM JobApplication ja " +
            "WHERE ja.job.company.recruiter.id = :recruiterId " +
            "AND ja.isWithdrawn = false")
    Integer countJobsWithApplications(@Param("recruiterId") Long recruiterId);

    @Query("SELECT COUNT(ja) FROM JobApplication ja " +
            "WHERE ja.job.id = :jobId AND ja.appliedAt >= :sinceDate")
    Long countByJobIdAndAppliedAtAfter(@Param("jobId") Long jobId,
                                       @Param("sinceDate") LocalDateTime sinceDate);

    // Check if candidate has applied to job
    boolean existsByCandidateIdAndJobId(Long candidateId, Long jobId);
}