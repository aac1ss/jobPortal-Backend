package com.jobportal.backend.dto.candidate.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LanguageRequest {
    @NotBlank(message = "Language is required")
    private String language;

    @NotBlank(message = "Proficiency is required")
    private String proficiency;
}
