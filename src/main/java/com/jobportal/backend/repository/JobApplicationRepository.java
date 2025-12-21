package com.jobportal.backend.repository;

import com.jobportal.backend.entity.JobApplication;
import com.jobportal.backend.enums.ApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {

    Optional<JobApplication> findByJobIdAndCandidateId(Long jobId, Long candidateId);

    List<JobApplication> findByCandidateId(Long candidateId);

    Page<JobApplication> findByCandidateId(Long candidateId, Pageable pageable);

    List<JobApplication> findByJobId(Long jobId);

    @Query("SELECT COUNT(ja) FROM JobApplication ja WHERE ja.candidate.id = :candidateId")
    Integer countByCandidateId(@Param("candidateId") Long candidateId);

    @Query("SELECT COUNT(ja) FROM JobApplication ja WHERE ja.candidate.id = :candidateId AND ja.status = :status")
    Integer countByCandidateIdAndStatus(@Param("candidateId") Long candidateId,
                                        @Param("status") ApplicationStatus status);

    boolean existsByJobIdAndCandidateId(Long jobId, Long candidateId);

    @Query("SELECT ja FROM JobApplication ja WHERE ja.candidate.id = :candidateId " +
            "AND ja.isWithdrawn = false ORDER BY ja.appliedAt DESC LIMIT :limit")
    List<JobApplication> findRecentByCandidateId(@Param("candidateId") Long candidateId,
                                                 @Param("limit") int limit);

    @Query("SELECT ja FROM JobApplication ja WHERE ja.candidate.user.id = :userId AND ja.isWithdrawn = false")
    List<JobApplication> findByUserId(@Param("userId") Long userId);

    @Query("SELECT ja FROM JobApplication ja WHERE ja.candidate.user.id = :userId AND ja.job.id = :jobId AND ja.isWithdrawn = false")
    Optional<JobApplication> findByUserIdAndJobId(@Param("userId") Long userId, @Param("jobId") Long jobId);
}