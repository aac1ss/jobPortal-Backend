package com.jobportal.backend.dto.company.request;

import com.jobportal.backend.enums.ApplicationStatus;
import lombok.Data;

@Data
public class UpdateApplicationStatusRequest {
    private ApplicationStatus status;
    private String notes; // Optional notes for status change
}
