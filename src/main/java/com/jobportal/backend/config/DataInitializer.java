package com.jobportal.backend.config;

import com.jobportal.backend.entity.Industry;
import com.jobportal.backend.enums.IndustryEnum;
import com.jobportal.backend.repository.IndustryRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Arrays;
import java.util.stream.Collectors;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {

    private final IndustryRepository industryRepository;

    @PostConstruct
    @Transactional
    public void init() {
        log.info("Starting industries initialization...");
        initIndustries();
        log.info("Industries initialization completed.");
    }

    private void initIndustries() {
        log.info("Initializing industries...");
        for (IndustryEnum industryEnum : IndustryEnum.values()) {
            String industryName = industryEnum.name();

            if (!industryRepository.existsByName(industryName)) {
                Industry industry = new Industry();
                industry.setName(industryName);
                industryRepository.save(industry);
                log.info("Created industry: {}", industryName);
            }
        }

        log.info("Industries initialization completed.");
    }

}