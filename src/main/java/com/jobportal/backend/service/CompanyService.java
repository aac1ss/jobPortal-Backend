package com.jobportal.backend.service;

import com.jobportal.backend.entity.Company;
import java.util.List;

public interface CompanyService {
    List<Company> getPublicCompanies();
}