package com.myspringproject.carwash.booking_service.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class WasherClientService {

    private final WebClient washerWebClient;

    public WasherClientService(WebClient washerWebClient) {
        this.washerWebClient = washerWebClient;
    }

    private static final Logger logger = LoggerFactory.getLogger(WasherClientService.class);
    /**
     * Fetches available washer IDs from the Washer Service.
     * Uses Resilience4j for circuit breaking and retrying.
     *
     * @return List of UUIDs representing available washers
     */
    @CircuitBreaker(name = "washerService", fallbackMethod = "fallbackAvailableWasherIds")
    @Retry(name = "washerService")
    public List<UUID> getAvailableWasherIds() {
        return washerWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/washer/available")
                        .build())
                .retrieve()
                .bodyToFlux(UUID.class) // directly parse as UUID
                .collectList()
                .block(); // sync for now
    }

    // Fallback if Washer Service is down
    @SuppressWarnings("unused")
    private List<UUID> fallbackAvailableWasherIds(Throwable ex) {
        logger.error("Fallback triggered for available washer IDs due to: {}",ex.getMessage());
        return Collections.emptyList();
    }
}
