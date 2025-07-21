package com.myspringproject.carwash.auth_service.service;

import java.time.LocalDateTime;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.myspringproject.carwash.auth_service.repository.TokenRepository;

@Component
public class TokenCleanupScheduler {

    private TokenRepository tokenRepository;

    TokenCleanupScheduler(TokenRepository repo){
        this.tokenRepository = repo;
    }

    @Scheduled(cron = "0 0 * * * *") // Every hour
    public void cleanExpiredTokens() {
        tokenRepository.deleteAllExpiredSince(LocalDateTime.now());
    }
}
