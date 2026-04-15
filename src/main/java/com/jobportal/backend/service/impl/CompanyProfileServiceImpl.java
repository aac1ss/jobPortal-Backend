package com.jobportal.backend.service.impl;

import com.jobportal.backend.dto.company.request.AdminCompanyFilter;
import com.jobportal.backend.dto.company.request.CompanyProfileRequest;
import com.jobportal.backend.dto.company.response.CompanyProfileResponse;
import com.jobportal.backend.dto.company.response.CompanyProfileStatusResponse;
import com.jobportal.backend.entity.CompanyProfile;
import com.jobportal.backend.entity.Industry;
import com.jobportal.backend.entity.User;
import com.jobportal.backend.enums.RoleEnum;
import com.jobportal.backend.exception.ProfileAlreadyExistsException;
import com.jobportal.backend.exception.ProfileNotFoundException;
import com.jobportal.backend.exception.UnauthorizedAccessException;
import com.jobportal.backend.exception.ValidationException;
import com.jobportal.backend.repository.CompanyProfileRepository;
import com.jobportal.backend.repository.IndustryRepository;
import com.jobportal.backend.repository.UserRepository;
import com.jobportal.backend.service.CompanyProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompanyProfileServiceImpl implements CompanyProfileService {

    private final CompanyProfileRepository companyProfileRepository;
    private final UserRepository userRepository;
    private final IndustryRepository industryRepository;

    @Override
    @Transactional
    public CompanyProfileResponse createProfile(Long recruiterId, CompanyProfileRequest request) {
        log.info("Creating company profile for recruiter ID: {}", recruiterId);

        User recruiter = userRepository.findById(recruiterId)
                .orElseThrow(() -> new ValidationException("Recruiter not found"));

        if (!recruiter.hasRole(RoleEnum.RECRUITER)) {
            throw new UnauthorizedAccessException("Only recruiters can create company profiles");
        }

        if (companyProfileRepository.existsByRecruiterId(recruiterId)) {
            throw new ProfileAlreadyExistsException("Company profile already exists for this recruiter");
        }

        if (companyProfileRepository.existsByCompanyName(request.getCompanyName())) {
            throw new ValidationException("Company name already exists");
        }

        // Get or create industry from enum name
        Industry industry = industryRepository.findByName(request.getIndustry().name())
                .orElseGet(() -> {
                    // Create if doesn't exist (should exist from DataInitializer)
                    Industry newIndustry = new Industry();
                    newIndustry.setName(request.getIndustry().name());
                    return industryRepository.save(newIndustry);
                });

        CompanyProfile profile = new CompanyProfile();
        mapRequestToEntity(request, profile, industry);
        profile.setRecruiter(recruiter);
        profile.setActive(true);  // New company is active by default
        profile.setVerified(false); // Not verified until admin approves
        profile.setCreatedAt(LocalDateTime.now());
        profile.setUpdatedAt(LocalDateTime.now());

        CompanyProfile savedProfile = companyProfileRepository.save(profile);
        log.info("Company profile created with ID: {}", savedProfile.getId());

        return mapEntityToResponse(savedProfile);
    }

    @Override
    @Transactional
    public CompanyProfileResponse updateProfile(Long recruiterId, CompanyProfileRequest request) {
        log.info("Updating company profile for recruiter ID: {}", recruiterId);

        CompanyProfile profile = companyProfileRepository.findByRecruiterId(recruiterId)
                .orElseThrow(() -> new ProfileNotFoundException("Company profile not found"));

        // Check if company name changed and if it's unique
        if (!profile.getCompanyName().equals(request.getCompanyName()) &&
                companyProfileRepository.existsByCompanyName(request.getCompanyName())) {
            throw new ValidationException("Company name already exists");
        }

        // Get or create industry
        Industry industry = industryRepository.findByName(request.getIndustry().name())
                .orElseGet(() -> {
                    Industry newIndustry = new Industry();
                    newIndustry.setName(request.getIndustry().name());
                    return industryRepository.save(newIndustry);
                });

        mapRequestToEntity(request, profile, industry);
        profile.setUpdatedAt(LocalDateTime.now());

        // If profile becomes incomplete after update, unverify it
        if (!profile.isProfileComplete() && profile.isVerified()) {
            profile.setVerified(false);
            profile.setVerifiedAt(null);
            log.info("Company profile ID: {} became incomplete, auto-unverified", profile.getId());
        }

        CompanyProfile updatedProfile = companyProfileRepository.save(profile);
        log.info("Company profile updated for recruiter ID: {}", recruiterId);

        return mapEntityToResponse(updatedProfile);
    }

    @Override
    @Transactional(readOnly = true)
    public CompanyProfileResponse getProfile(Long recruiterId) {
        log.debug("Getting company profile for recruiter ID: {}", recruiterId);

        CompanyProfile profile = companyProfileRepository.findByRecruiterId(recruiterId)
                .orElseThrow(() -> new ProfileNotFoundException("Company profile not found"));

        return mapEntityToResponse(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public CompanyProfileResponse getProfileById(Long profileId) {
        log.debug("Getting company profile by ID: {}", profileId);

        // Admin access - no restrictions
        CompanyProfile profile = companyProfileRepository.findById(profileId)
                .orElseThrow(() -> new ProfileNotFoundException("Company profile not found"));

        return mapEntityToResponse(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public CompanyProfileResponse getPublicProfileById(Long profileId) {
        log.debug("Getting public company profile by ID: {}", profileId);

        CompanyProfile profile = companyProfileRepository.findById(profileId)
                .orElseThrow(() -> new ProfileNotFoundException("Company profile not found"));

        // PUBLIC ACCESS - only verified and active companies
        if (!profile.isVisibleToPublic()) {
            throw new ProfileNotFoundException("Company profile not found or not visible");
        }

        return mapEntityToResponse(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public CompanyProfileStatusResponse getProfileStatus(Long recruiterId) {
        log.debug("Getting profile status for recruiter ID: {}", recruiterId);

        User recruiter = userRepository.findById(recruiterId)
                .orElseThrow(() -> new ValidationException("Recruiter not found"));

        if (!recruiter.hasRole(RoleEnum.RECRUITER)) {
            throw new UnauthorizedAccessException("Only recruiters have company profiles");
        }

        // Check if profile exists
        if (!recruiter.hasCompanyProfile()) {
            return new CompanyProfileStatusResponse(
                    false, false, false,false, 0,
                    new String[]{"companyName", "industry", "companySize", "description",
                            "contactName", "contactEmail", "city", "country"}
            );
        }

        CompanyProfile profile = recruiter.getCompanyProfile();

        // Determine missing fields
        List<String> missingFields = new ArrayList<>();
        if (profile.getCompanyName() == null || profile.getCompanyName().trim().isEmpty()) {
            missingFields.add("companyName");
        }
        if (profile.getIndustry() == null) {
            missingFields.add("industry");
        }
        if (profile.getCompanySize() == null || profile.getCompanySize().trim().isEmpty()) {
            missingFields.add("companySize");
        }
        if (profile.getDescription() == null || profile.getDescription().length() < 100) {
            missingFields.add("description");
        }
        if (profile.getContactName() == null || profile.getContactName().trim().isEmpty()) {
            missingFields.add("contactName");
        }
        if (profile.getContactEmail() == null ||
                !profile.getContactEmail().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            missingFields.add("contactEmail");
        }
        if (profile.getCity() == null || profile.getCity().trim().isEmpty()) {
            missingFields.add("city");
        }
        if (profile.getCountry() == null || profile.getCountry().trim().isEmpty()) {
            missingFields.add("country");
        }

        return new CompanyProfileStatusResponse(
                true,
                profile.isProfileComplete(),
                profile.isVerified(),
                profile.isActive(),
                profile.getCompletionPercentage(),
                missingFields.toArray(new String[0])
        );
    }

    @Override
    @Transactional
    public void deleteProfile(Long recruiterId) {
        log.info("Deleting company profile for recruiter ID: {}", recruiterId);

        CompanyProfile profile = companyProfileRepository.findByRecruiterId(recruiterId)
                .orElseThrow(() -> new ProfileNotFoundException("Company profile not found"));

        // Soft delete - set isActive to false
        profile.setActive(false);
        profile.setUpdatedAt(LocalDateTime.now());

        companyProfileRepository.save(profile);
        log.info("Company profile deleted for recruiter ID: {}", recruiterId);
    }

    @Override
    @Transactional
    public CompanyProfileResponse verifyProfile(Long profileId, Long adminId) {
        log.info("Verifying company profile ID: {} by admin ID: {}", profileId, adminId);

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ValidationException("Admin not found"));

        if (!admin.hasRole(RoleEnum.ADMIN)) {
            throw new UnauthorizedAccessException("Only admins can verify company profiles");
        }

        CompanyProfile profile = companyProfileRepository.findById(profileId)
                .orElseThrow(() -> new ProfileNotFoundException("Company profile not found"));

        if (!profile.isProfileComplete()) {
            throw new ValidationException("Cannot verify incomplete company profile");
        }

        if (!profile.isActive()) {
            throw new ValidationException("Cannot verify inactive company profile");
        }

        profile.verify();
        profile.setUpdatedAt(LocalDateTime.now());

        CompanyProfile verifiedProfile = companyProfileRepository.save(profile);
        log.info("Company profile ID: {} verified by admin ID: {}", profileId, adminId);

        return mapEntityToResponse(verifiedProfile);
    }

    @Override
    @Transactional
    public CompanyProfileResponse unverifyProfile(Long profileId, Long adminId) {
        log.info("Unverifying company profile ID: {} by admin ID: {}", profileId, adminId);

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ValidationException("Admin not found"));

        if (!admin.hasRole(RoleEnum.ADMIN)) {
            throw new UnauthorizedAccessException("Only admins can unverify company profiles");
        }

        CompanyProfile profile = companyProfileRepository.findById(profileId)
                .orElseThrow(() -> new ProfileNotFoundException("Company profile not found"));

        profile.unverify();
        profile.setUpdatedAt(LocalDateTime.now());

        CompanyProfile unverifiedProfile = companyProfileRepository.save(profile);
        log.info("Company profile ID: {} unverified by admin ID: {}", profileId, adminId);

        return mapEntityToResponse(unverifiedProfile);
    }

    @Override
    @Transactional
    public CompanyProfileResponse activateProfile(Long profileId, Long adminId) {
        log.info("Activating company profile ID: {} by admin ID: {}", profileId, adminId);

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ValidationException("Admin not found"));

        if (!admin.hasRole(RoleEnum.ADMIN)) {
            throw new UnauthorizedAccessException("Only admins can activate company profiles");
        }

        CompanyProfile profile = companyProfileRepository.findById(profileId)
                .orElseThrow(() -> new ProfileNotFoundException("Company profile not found"));

        profile.setActive(true);
        profile.setUpdatedAt(LocalDateTime.now());

        CompanyProfile activatedProfile = companyProfileRepository.save(profile);
        log.info("Company profile ID: {} activated by admin ID: {}", profileId, adminId);

        return mapEntityToResponse(activatedProfile);
    }

    @Override
    @Transactional
    public CompanyProfileResponse deactivateProfile(Long profileId, Long adminId) {
        log.info("Deactivating company profile ID: {} by admin ID: {}", profileId, adminId);

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ValidationException("Admin not found"));

        if (!admin.hasRole(RoleEnum.ADMIN)) {
            throw new UnauthorizedAccessException("Only admins can deactivate company profiles");
        }

        CompanyProfile profile = companyProfileRepository.findById(profileId)
                .orElseThrow(() -> new ProfileNotFoundException("Company profile not found"));

        profile.setActive(false);
        profile.setUpdatedAt(LocalDateTime.now());

        CompanyProfile deactivatedProfile = companyProfileRepository.save(profile);
        log.info("Company profile ID: {} deactivated by admin ID: {}", profileId, adminId);

        return mapEntityToResponse(deactivatedProfile);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CompanyProfileResponse> getAllVerifiedCompanies(Pageable pageable) {
        log.debug("Getting all verified companies");

        // Public endpoint - only active and verified
        Page<CompanyProfile> companies = companyProfileRepository.findByIsActiveAndIsVerified(
                true, true, pageable
        );

        return companies.map(this::mapEntityToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CompanyProfileResponse> getAllCompaniesForAdmin(Pageable pageable) {
        log.debug("Getting all companies for admin");

        // SIMPLE: Just use findAll() with pageable
        Page<CompanyProfile> companies = companyProfileRepository.findAll(pageable);

        return companies.map(this::mapEntityToResponse);
    }


    @Override
    @Transactional(readOnly = true)
    public boolean isProfileComplete(Long recruiterId) {
        return companyProfileRepository.findByRecruiterId(recruiterId)
                .map(CompanyProfile::isProfileComplete)
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canPostJobs(Long recruiterId) {
        return companyProfileRepository.findByRecruiterId(recruiterId)
                .map(CompanyProfile::canPostJobs)
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isCompanyVerified(Long companyId) {
        return companyProfileRepository.findById(companyId)
                .map(CompanyProfile::isVerified)
                .orElse(false);
    }

    // Helper methods
    private void mapRequestToEntity(CompanyProfileRequest request, CompanyProfile entity, Industry industry) {
        entity.setCompanyName(request.getCompanyName());
        entity.setDisplayName(request.getDisplayName());
        entity.setIndustry(industry);
        entity.setCompanySize(request.getCompanySize());
        entity.setWebsite(request.getWebsite());
        entity.setDescription(request.getDescription());
        entity.setContactName(request.getContactName());
        entity.setContactEmail(request.getContactEmail());
        entity.setContactPhone(request.getContactPhone());
        entity.setAddress(request.getAddress());
        entity.setCity(request.getCity());
        entity.setState(request.getState());
        entity.setCountry(request.getCountry());
        entity.setPostalCode(request.getPostalCode());
        entity.setLogoUrl(request.getLogoUrl());
        entity.setCoverImageUrl(request.getCoverImageUrl());
        entity.setLinkedinUrl(request.getLinkedinUrl());
        entity.setTwitterUrl(request.getTwitterUrl());
        entity.setFacebookUrl(request.getFacebookUrl());
    }

    private CompanyProfileResponse mapEntityToResponse(CompanyProfile entity) {
        CompanyProfileResponse response = new CompanyProfileResponse();
        response.setId(entity.getId());
        response.setCompanyName(entity.getCompanyName());
        response.setDisplayName(entity.getDisplayName());

        // Map industry info
        if (entity.getIndustry() != null) {
            response.setIndustryId(entity.getIndustry().getId());
            response.setIndustryName(entity.getIndustry().getName());
        }

        response.setCompanySize(entity.getCompanySize());
        response.setWebsite(entity.getWebsite());
        response.setDescription(entity.getDescription());
        response.setContactName(entity.getContactName());
        response.setContactEmail(entity.getContactEmail());
        response.setContactPhone(entity.getContactPhone());
        response.setAddress(entity.getAddress());
        response.setCity(entity.getCity());
        response.setState(entity.getState());
        response.setCountry(entity.getCountry());
        response.setPostalCode(entity.getPostalCode());
        response.setLogoUrl(entity.getLogoUrl());
        response.setCoverImageUrl(entity.getCoverImageUrl());
        response.setVerified(entity.isVerified());
        response.setActive(entity.isActive());
        response.setProfileComplete(entity.isProfileComplete());
        response.setCompletionPercentage(entity.getCompletionPercentage());
        response.setLinkedinUrl(entity.getLinkedinUrl());
        response.setTwitterUrl(entity.getTwitterUrl());
        response.setFacebookUrl(entity.getFacebookUrl());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        response.setVerifiedAt(entity.getVerifiedAt());

        if (entity.getRecruiter() != null) {
            response.setRecruiterId(entity.getRecruiter().getId());
            response.setRecruiterName(entity.getRecruiter().getFullName());
            response.setRecruiterEmail(entity.getRecruiter().getEmail());
        }

        return response;
    }
}