package com.jobportal.backend.dto;

public class RecommendationScore {
        private double totalScore;
        private double skillScore;
        private double experienceScore;
        private double locationScore;
        private double jobTypeScore;
        private double salaryScore;
        private double trendingScore;
        private double recencyScore;

        // Getters and setters
        public double getTotalScore() { return totalScore; }
        public void setTotalScore(double totalScore) { this.totalScore = totalScore; }
        public double getSkillScore() { return skillScore; }
        public void setSkillScore(double skillScore) { this.skillScore = skillScore; }
        public double getExperienceScore() { return experienceScore; }
        public void setExperienceScore(double experienceScore) { this.experienceScore = experienceScore; }
        public double getLocationScore() { return locationScore; }
        public void setLocationScore(double locationScore) { this.locationScore = locationScore; }
        public double getJobTypeScore() { return jobTypeScore; }
        public void setJobTypeScore(double jobTypeScore) { this.jobTypeScore = jobTypeScore; }
        public double getSalaryScore() { return salaryScore; }
        public void setSalaryScore(double salaryScore) { this.salaryScore = salaryScore; }
        public double getTrendingScore() { return trendingScore; }
        public void setTrendingScore(double trendingScore) { this.trendingScore = trendingScore; }
        public double getRecencyScore() { return recencyScore; }
        public void setRecencyScore(double recencyScore) { this.recencyScore = recencyScore; }
    }
