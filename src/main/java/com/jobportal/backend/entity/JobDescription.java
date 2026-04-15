package com.jobportal.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "job_descriptions")
@Data
@EqualsAndHashCode(exclude = {"job"})
public class JobDescription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "overview", columnDefinition = "TEXT", nullable = false)
    private String overview;

    @Column(name = "responsibilities", columnDefinition = "TEXT", nullable = false)
    private String responsibilities;

    @Column(name = "requirements", columnDefinition = "TEXT", nullable = false)
    private String requirements;

    @Column(name = "nice_to_have", columnDefinition = "TEXT")
    private String niceToHave;

    @Column(name = "benefits", columnDefinition = "TEXT")
    private String benefits;

    @Column(name = "application_instructions", columnDefinition = "TEXT")
    private String applicationInstructions;

    // One-to-one relationship with Job
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false, unique = true)
    private Job job;
}