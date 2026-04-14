package com.jobportal.backend.dto.candidate;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendedJob {
    private Long id;
    private String title;
    private String companyName;
    private String location;
    private String jobType;
    private Integer matchScore;
    private Boolean isRemote;
    private String salaryRange;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime postedDate;
    private Boolean hasApplied;
}
