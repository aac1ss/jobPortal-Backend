package com.jobportal.backend.service;


import com.jobportal.backend.dto.security.request.CompanyProfileRequest;
import com.jobportal.backend.dto.security.response.CompanyProfileResponse;
import com.jobportal.backend.dto.security.response.CompanyProfileStatusResponse;

public interface CompanyProfileService {

    CompanyProfileResponse createProfile(Long recruiterId, CompanyProfileRequest request);

    CompanyProfileResponse updateProfile(Long recruiterId, CompanyProfileRequest request);

    CompanyProfileResponse getProfile(Long recruiterId);

    CompanyProfileResponse getProfileById(Long profileId);

    CompanyProfileStatusResponse getProfileStatus(Long recruiterId);

    void deleteProfile(Long recruiterId);

    CompanyProfileResponse verifyProfile(Long profileId, Long adminId);

    CompanyProfileResponse unverifyProfile(Long profileId, Long adminId);

    boolean isProfileComplete(Long recruiterId);

    boolean canPostJobs(Long recruiterId);
}