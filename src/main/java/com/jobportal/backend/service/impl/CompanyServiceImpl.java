package com.jobportal.backend.service.impl;

import com.jobportal.backend.entity.Company;
import com.jobportal.backend.repository.CompanyRepository;
import com.jobportal.backend.service.CompanyService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CompanyServiceImpl implements CompanyService {

    private final CompanyRepository companyRepository;

    public CompanyServiceImpl(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    @Override
    public List<Company> getPublicCompanies() {
        // Implement logic to get public companies
        return companyRepository.findAll();
    }
}