package com.jobportal.backend.dto.candidate;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public  class RecentApplication {
    private Long id;
    private String jobTitle;
    private String companyName;
    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private String appliedDate;

    private Integer matchScore;
}
