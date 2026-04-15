package com.jobportal.backend.dto.candidate.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExperienceResponse {
    private String company;
    private String position;
    private String employmentType;
    private String location;
    private Boolean isCurrent;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    private String description;
    private String achievements;
    private List<String> skillsUsed;
    private Integer experienceInMonths;
}

