package com.jobportal.backend.dto.company.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Company filter request for admin")
public class AdminCompanyFilter {

    @Schema(description = "Verification status: 'verified', 'unverified', or 'all' (default)",
            example = "all", defaultValue = "all")
    private String verificationStatus = "all";

    @Schema(description = "Search term for company name", example = "Tech")
    private String search;

    @Schema(description = "Sort direction: 'ASC' or 'DESC'",
            example = "DESC", defaultValue = "DESC")
    private String sortDirection = "DESC";

    @Schema(description = "Sort by: 'createdAt' or 'companyName'",
            example = "createdAt", defaultValue = "createdAt")
    private String sortBy = "createdAt";
}