package com.myspringproject.carwash.api_gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "carwash.services")
public record ServiceRouteProperties(
        String authUrl,
        String customerUrl,
        String bookingUrl,
        String washerUrl,
        String paymentUrl) {
}
