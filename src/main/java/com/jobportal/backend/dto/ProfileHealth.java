package com.jobportal.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public  class ProfileHealth {
    private Integer completionPercentage;
    private List<String> suggestions;
    private Map<String, Boolean> checkpoints;
}
