package com.jobportal.backend.service;

import com.jobportal.backend.entity.CandidateProfile;
import com.jobportal.backend.entity.Job;

public interface MatchScoreService {
    int calculateMatchScore(CandidateProfile candidate, Job job);
    String generateMatchNotes(CandidateProfile candidate, Job job, int score);
}