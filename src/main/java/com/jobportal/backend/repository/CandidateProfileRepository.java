package com.jobportal.backend.repository;

import com.jobportal.backend.entity.CandidateProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CandidateProfileRepository extends JpaRepository<CandidateProfile, Long> {

    Optional<CandidateProfile> findByUserId(Long userId);

    Optional<CandidateProfile> findByEmail(String email);

    boolean existsByUserId(Long userId);

    boolean existsByEmail(String email);

    @Query("SELECT cp FROM CandidateProfile cp WHERE cp.isProfileComplete = true AND cp.isActivelyLooking = true AND cp.user.isActive = true")
    Page<CandidateProfile> findActiveCandidates(Pageable pageable);

    @Query("SELECT cp FROM CandidateProfile cp WHERE " +
            "(:skill IS NULL OR LOWER(cp.skills) LIKE LOWER(CONCAT('%', :skill, '%'))) AND " +
            "(:location IS NULL OR LOWER(cp.preferredLocationsJson) LIKE LOWER(CONCAT('%', :location, '%'))) AND " +
            "cp.isProfileComplete = true AND cp.isActivelyLooking = true AND cp.user.isActive = true")
    Page<CandidateProfile> searchCandidates(@Param("skill") String skill,
                                            @Param("location") String location,
                                            Pageable pageable);

    @Query("SELECT COUNT(cp) FROM CandidateProfile cp WHERE cp.isProfileComplete = true")
    Long countCompleteProfiles();

    @Query("SELECT cp FROM CandidateProfile cp WHERE cp.user.isActive = true AND cp.isProfileComplete = true")
    Page<CandidateProfile> findAllActiveCompleteProfiles(Pageable pageable);


    Long countByIsActivelyLookingTrue();
}

