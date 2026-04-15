package com.jobportal.backend.service;

import com.jobportal.backend.dto.company.request.AdminCompanyFilter;
import com.jobportal.backend.dto.company.request.CompanyProfileRequest;
import com.jobportal.backend.dto.company.response.CompanyProfileResponse;
import com.jobportal.backend.dto.company.response.CompanyProfileStatusResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CompanyProfileService {

    CompanyProfileResponse createProfile(Long recruiterId, CompanyProfileRequest request);

    CompanyProfileResponse updateProfile(Long recruiterId, CompanyProfileRequest request);

    CompanyProfileResponse getProfile(Long recruiterId);

    CompanyProfileResponse getProfileById(Long profileId);

    CompanyProfileStatusResponse getProfileStatus(Long recruiterId);

    void deleteProfile(Long recruiterId);

    // Admin methods
    CompanyProfileResponse verifyProfile(Long profileId, Long adminId);

    CompanyProfileResponse unverifyProfile(Long profileId, Long adminId);

    CompanyProfileResponse activateProfile(Long profileId, Long adminId);

    CompanyProfileResponse deactivateProfile(Long profileId, Long adminId);

    Page<CompanyProfileResponse> getAllCompaniesForAdmin(Pageable pageable);

    Page<CompanyProfileResponse> getAllVerifiedCompanies(Pageable pageable);

    boolean isProfileComplete(Long recruiterId);

    boolean canPostJobs(Long recruiterId);

    CompanyProfileResponse getPublicProfileById(Long profileId); // For public - only verified

    boolean isCompanyVerified(Long companyId);
}