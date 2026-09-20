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
public class ResumeUploadRequest {

    @NotBlank(message = "Resume URL is required")
    private String resumeUrl;

    @NotBlank(message = "File name is required")
    private String fileName;

    @NotBlank(message = "Original file name is required")
    private String originalFileName;
}
