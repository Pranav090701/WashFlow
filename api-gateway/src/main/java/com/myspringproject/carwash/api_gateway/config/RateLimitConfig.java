package com.myspringproject.carwash.api_gateway.config;

import java.net.InetSocketAddress;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

@Configuration
public class RateLimitConfig {

    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> Mono.just(resolveClientIp(exchange));
    }

    @Bean
    @Primary
    public KeyResolver userIdKeyResolver(@Qualifier("ipKeyResolver") KeyResolver ipKeyResolver) {
        return exchange -> exchange.getPrincipal()
                .cast(JwtAuthenticationToken.class)
                .map(authentication -> authentication.getToken().getSubject())
                .filter(subject -> subject != null && !subject.isBlank())
                .switchIfEmpty(ipKeyResolver.resolve(exchange));
    }

    @Bean
    public RedisRateLimiter authLoginRateLimiter(RateLimitProperties properties) {
        return redisRateLimiter(properties.authLogin());
    }

    @Bean
    public RedisRateLimiter authRegisterRateLimiter(RateLimitProperties properties) {
        return redisRateLimiter(properties.authRegister());
    }

    @Bean
    public RedisRateLimiter authResendVerificationRateLimiter(RateLimitProperties properties) {
        return redisRateLimiter(properties.authResendVerification());
    }

    @Bean
    public RedisRateLimiter slotLockRateLimiter(RateLimitProperties properties) {
        return redisRateLimiter(properties.slotLock());
    }

    @Bean
    public RedisRateLimiter paymentInitiateRateLimiter(RateLimitProperties properties) {
        return redisRateLimiter(properties.paymentInitiate());
    }

    @Bean
    public RedisRateLimiter paymentVerifyRateLimiter(RateLimitProperties properties) {
        return redisRateLimiter(properties.paymentVerify());
    }

    @Bean
    @Primary
    public RedisRateLimiter defaultUserRateLimiter(RateLimitProperties properties) {
        return redisRateLimiter(properties.defaultUser());
    }

    private RedisRateLimiter redisRateLimiter(RateLimitProperties.Rule rule) {
        return new RedisRateLimiter(
                rule.replenishRate(),
                rule.burstCapacity(),
                rule.requestedTokens());
    }

    private static String resolveClientIp(ServerWebExchange exchange) {
        String forwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return "ip:" + forwardedFor.split(",")[0].trim();
        }

        InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
        if (remoteAddress != null && remoteAddress.getAddress() != null) {
            return "ip:" + remoteAddress.getAddress().getHostAddress();
        }

        return "ip:unknown";
    }
}
