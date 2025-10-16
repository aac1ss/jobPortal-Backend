package com.jobportal.backend.repository;

import com.jobportal.backend.entity.RefreshToken;
import com.jobportal.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);
    Optional<RefreshToken> findByUser(User user);

    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId); // Changed to void

    // Remove the old deleteByUser method since we're not using it
    // @Modifying
    // @Query("DELETE FROM RefreshToken rt WHERE rt.user = :user")
    // int deleteByUser(@Param("user") User user);
}