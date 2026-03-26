//package com.jobportal.backend.util;
//
//import com.jobportal.backend.entity.CompanyProfile;
//import org.springframework.data.jpa.domain.Specification;
//import org.springframework.util.StringUtils;
//
//import java.time.LocalDateTime;
//
//public class CompanyProfileSpecification {
//
//    public static Specification<CompanyProfile> withVerificationStatus(String status) {
//        return (root, query, criteriaBuilder) -> {
//            if (!StringUtils.hasText(status) || "all".equalsIgnoreCase(status)) {
//                return criteriaBuilder.conjunction(); // Return all
//            }
//
//            if ("verified".equalsIgnoreCase(status)) {
//                return criteriaBuilder.isTrue(root.get("isVerified"));
//            } else if ("unverified".equalsIgnoreCase(status)) {
//                return criteriaBuilder.isFalse(root.get("isVerified"));
//            }
//
//            return criteriaBuilder.conjunction(); // Default to all
//        };
//    }
//
//    public static Specification<CompanyProfile> withActiveStatus(String status) {
//        return (root, query, criteriaBuilder) -> {
//            if (!StringUtils.hasText(status) || "all".equalsIgnoreCase(status)) {
//                return criteriaBuilder.conjunction(); // Return all
//            }
//
//            if ("active".equalsIgnoreCase(status)) {
//                return criteriaBuilder.isTrue(root.get("isActive"));
//            } else if ("inactive".equalsIgnoreCase(status)) {
//                return criteriaBuilder.isFalse(root.get("isActive"));
//            }
//
//            return criteriaBuilder.conjunction(); // Default to all
//        };
//    }
//
//    public static Specification<CompanyProfile> withCompanyName(String companyName) {
//        return (root, query, criteriaBuilder) -> {
//            if (!StringUtils.hasText(companyName)) {
//                return criteriaBuilder.conjunction();
//            }
//            return criteriaBuilder.like(
//                    criteriaBuilder.lower(root.get("companyName")),
//                    "%" + companyName.toLowerCase() + "%"
//            );
//        };
//    }
//
//    public static Specification<CompanyProfile> withIndustryId(Long industryId) {
//        return (root, query, criteriaBuilder) -> {
//            if (industryId == null) {
//                return criteriaBuilder.conjunction();
//            }
//            return criteriaBuilder.equal(root.get("industry").get("id"), industryId);
//        };
//    }
//
//    public static Specification<CompanyProfile> withCountry(String country) {
//        return (root, query, criteriaBuilder) -> {
//            if (!StringUtils.hasText(country)) {
//                return criteriaBuilder.conjunction();
//            }
//            return criteriaBuilder.like(
//                    criteriaBuilder.lower(root.get("country")),
//                    "%" + country.toLowerCase() + "%"
//            );
//        };
//    }
//
//    public static Specification<CompanyProfile> createdAfter(LocalDateTime date) {
//        return (root, query, criteriaBuilder) -> {
//            if (date == null) {
//                return criteriaBuilder.conjunction();
//            }
//            return criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), date);
//        };
//    }
//
//    public static Specification<CompanyProfile> createdBefore(LocalDateTime date) {
//        return (root, query, criteriaBuilder) -> {
//            if (date == null) {
//                return criteriaBuilder.conjunction();
//            }
//            return criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), date);
//        };
//    }
//}