package com.myspringproject.carwash.api_gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "carwash.rate-limit")
public record RateLimitProperties(
        Rule authLogin,
        Rule authRegister,
        Rule authResendVerification,
        Rule slotLock,
        Rule paymentInitiate,
        Rule paymentVerify,
        Rule defaultUser) {

    public record Rule(
            int replenishRate,
            int burstCapacity,
            int requestedTokens) {
    }
}
