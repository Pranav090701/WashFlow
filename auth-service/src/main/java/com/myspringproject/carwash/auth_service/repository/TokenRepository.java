package com.myspringproject.carwash.auth_service.repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;


import com.myspringproject.carwash.auth_service.entity.VerificationToken;

import jakarta.transaction.Transactional;

@Repository
public interface TokenRepository extends JpaRepository<VerificationToken,UUID>{
    
    Optional<VerificationToken> findByToken(String token);

    @Modifying
    @Transactional
    @Query("DELETE FROM VerificationToken t WHERE t.expiryDate < :now")
    void deleteAllExpiredSince(@Param("now") LocalDateTime now);
}
