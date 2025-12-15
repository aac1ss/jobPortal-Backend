package com.jobportal.backend.repository;

import com.jobportal.backend.entity.CompanyProfile;
import com.jobportal.backend.entity.Industry;
import com.jobportal.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompanyProfileRepository extends JpaRepository<CompanyProfile, Long> {

    Optional<CompanyProfile> findByRecruiter(User recruiter);
    Optional<CompanyProfile> findByRecruiterId(Long recruiterId);
    List<CompanyProfile> findByIsVerified(boolean isVerified);
    List<CompanyProfile> findByIndustry(Industry industry);
    List<CompanyProfile> findByCountry(String country);
    List<CompanyProfile> findByIsActive(boolean isActive);

    @Query("SELECT cp FROM CompanyProfile cp WHERE cp.profileComplete = true AND cp.isActive = true")
    List<CompanyProfile> findCompleteActiveProfiles();

    @Query("SELECT cp FROM CompanyProfile cp WHERE cp.companyName LIKE %:searchTerm% OR cp.description LIKE %:searchTerm%")
    List<CompanyProfile> searchByNameOrDescription(@Param("searchTerm") String searchTerm);

    boolean existsByRecruiterId(Long recruiterId);
    boolean existsByCompanyName(String companyName);

    @Query("SELECT cp FROM CompanyProfile cp WHERE cp.industry.id = :industryId")
    List<CompanyProfile> findByIndustryId(@Param("industryId") Long industryId);
}