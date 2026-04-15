package com.jobportal.backend.repository;

import com.jobportal.backend.entity.User;
import com.jobportal.backend.enums.RoleEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Boolean existsByEmail(String email);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.roleEnums WHERE u.email = :email AND u.deletedAt IS NULL AND u.isActive = true")
    Optional<User> findActiveByEmailWithRoles(@Param("email") String email);

    // Fixed: Use @Query for counting users by creation date
    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt >= :dateTime")
    Long countByCreatedAtAfter(@Param("dateTime") LocalDateTime dateTime);

    // Fixed: Count users by role (using the roleEnums collection)
    @Query("SELECT COUNT(DISTINCT u) FROM User u JOIN u.roleEnums r WHERE r = :role AND u.createdAt >= :dateTime")
    Long countByRoleAndCreatedAtAfter(@Param("role") RoleEnum role, @Param("dateTime") LocalDateTime dateTime);

    // Alternative: Count by role name as string
    @Query("SELECT COUNT(DISTINCT u) FROM User u JOIN u.roleEnums r WHERE r = :role AND u.createdAt >= :dateTime")
    Long countByRoleAndCreatedAtAfter(@Param("role") String role, @Param("dateTime") LocalDateTime dateTime);

    // Count total users by role
    @Query("SELECT COUNT(DISTINCT u) FROM User u JOIN u.roleEnums r WHERE r = :role")
    Long countByRole(@Param("role") RoleEnum role);
}