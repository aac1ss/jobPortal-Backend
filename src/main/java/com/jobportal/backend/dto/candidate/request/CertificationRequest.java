package com.jobportal.backend.dto.candidate.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CertificationRequest {
    @NotBlank(message = "Certification name is required")
    private String name;

    private String issuingOrganization;
    private String credentialId;
    private String credentialUrl;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate issueDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate expirationDate;

    private Boolean doesNotExpire = false;
}
