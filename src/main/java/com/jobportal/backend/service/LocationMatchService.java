package com.jobportal.backend.service;

import com.jobportal.backend.entity.CandidateProfile;
import com.jobportal.backend.entity.Job;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LocationMatchService {

    public int calculateLocationMatchScore(CandidateProfile candidate, Job job) {
        List<String> preferredLocations = candidate.getPreferredLocationsList();
        String jobLocation = job.getLocation().toLowerCase();

        // If job is remote and candidate prefers remote
        if (job.isRemote()) {
            Boolean remotePreference = candidate.getRemotePreference();
            if (remotePreference == null || remotePreference) {
                return 20; // Full score for remote match
            }
        }

        // Check location match if candidate has preferred locations
        if (preferredLocations != null && !preferredLocations.isEmpty()) {
            boolean locationMatch = preferredLocations.stream()
                    .anyMatch(loc -> isLocationMatch(loc.toLowerCase(), jobLocation));

            if (locationMatch) {
                return 20; // Full score for location match
            }
        }

        // If job is remote but candidate doesn't have remote preference
        if (job.isRemote()) {
            return 10; // Half score for remote jobs even without explicit preference
        }

        return 0; // No match
    }

    private boolean isLocationMatch(String preferredLocation, String jobLocation) {
        // Exact match
        if (preferredLocation.equals(jobLocation)) {
            return true;
        }

        // Check if one contains the other
        if (preferredLocation.contains(jobLocation) || jobLocation.contains(preferredLocation)) {
            return true;
        }

        // Handle common location variations
        if (preferredLocation.equals("remote") && jobLocation.contains("remote")) {
            return true;
        }

        if (preferredLocation.contains("new york") && jobLocation.contains("ny")) {
            return true;
        }

        if (preferredLocation.contains("san francisco") &&
                (jobLocation.contains("sf") || jobLocation.contains("bay area"))) {
            return true;
        }

        return false;
    }
}