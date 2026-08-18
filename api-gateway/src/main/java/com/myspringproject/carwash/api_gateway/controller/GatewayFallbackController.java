package com.myspringproject.carwash.api_gateway.controller;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

@RestController
public class GatewayFallbackController {

    @RequestMapping("/fallback/service-unavailable")
    public Mono<ResponseEntity<GatewayFallbackResponse>> serviceUnavailable(ServerWebExchange exchange) {
        GatewayFallbackResponse response = new GatewayFallbackResponse(
                Instant.now(),
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase(),
                "Downstream service is temporarily unavailable",
                exchange.getRequest().getPath().pathWithinApplication().value());
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response));
    }

    public record GatewayFallbackResponse(
            Instant timestamp,
            int status,
            String error,
            String message,
            String path) {
    }
}
