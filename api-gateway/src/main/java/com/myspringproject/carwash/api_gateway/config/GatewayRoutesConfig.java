package com.myspringproject.carwash.api_gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

@Configuration
public class GatewayRoutesConfig {

    @Bean
    public RouteLocator carWashRoutes(
            RouteLocatorBuilder builder,
            ServiceRouteProperties serviceRoutes,
            @Qualifier("ipKeyResolver") KeyResolver ipKeyResolver,
            @Qualifier("userIdKeyResolver") KeyResolver userIdKeyResolver,
            @Qualifier("authLoginRateLimiter") RedisRateLimiter authLoginRateLimiter,
            @Qualifier("authRegisterRateLimiter") RedisRateLimiter authRegisterRateLimiter,
            @Qualifier("authResendVerificationRateLimiter") RedisRateLimiter authResendVerificationRateLimiter,
            @Qualifier("slotLockRateLimiter") RedisRateLimiter slotLockRateLimiter,
            @Qualifier("paymentInitiateRateLimiter") RedisRateLimiter paymentInitiateRateLimiter,
            @Qualifier("paymentVerifyRateLimiter") RedisRateLimiter paymentVerifyRateLimiter,
            @Qualifier("defaultUserRateLimiter") RedisRateLimiter defaultUserRateLimiter) {
        return builder.routes()
                .route("auth-login", route -> route
                        .method(HttpMethod.POST)
                        .and()
                        .path("/auth/login")
                        .filters(filters -> filters.requestRateLimiter(config -> config
                                .setRateLimiter(authLoginRateLimiter)
                                .setKeyResolver(ipKeyResolver))
                                .circuitBreaker(config -> config
                                        .setName("authService")
                                        .setFallbackUri("forward:/fallback/service-unavailable")))
                        .uri(serviceRoutes.authUrl()))
                .route("auth-register", route -> route
                        .method(HttpMethod.POST)
                        .and()
                        .path("/auth/register")
                        .filters(filters -> filters.requestRateLimiter(config -> config
                                .setRateLimiter(authRegisterRateLimiter)
                                .setKeyResolver(ipKeyResolver))
                                .circuitBreaker(config -> config
                                        .setName("authService")
                                        .setFallbackUri("forward:/fallback/service-unavailable")))
                        .uri(serviceRoutes.authUrl()))
                .route("auth-resend-verification", route -> route
                        .method(HttpMethod.POST)
                        .and()
                        .path("/auth/resend-verification")
                        .filters(filters -> filters.requestRateLimiter(config -> config
                                .setRateLimiter(authResendVerificationRateLimiter)
                                .setKeyResolver(ipKeyResolver))
                                .circuitBreaker(config -> config
                                        .setName("authService")
                                        .setFallbackUri("forward:/fallback/service-unavailable")))
                        .uri(serviceRoutes.authUrl()))
                .route("auth-verify", route -> route
                        .method(HttpMethod.GET)
                        .and()
                        .path("/auth/verify")
                        .filters(filters -> filters.circuitBreaker(config -> config
                                .setName("authService")
                                .setFallbackUri("forward:/fallback/service-unavailable")))
                        .uri(serviceRoutes.authUrl()))
                .route("auth-service", route -> route
                        .path("/auth/**")
                        .filters(filters -> filters.requestRateLimiter(config -> config
                                .setRateLimiter(defaultUserRateLimiter)
                                .setKeyResolver(userIdKeyResolver))
                                .circuitBreaker(config -> config
                                        .setName("authService")
                                        .setFallbackUri("forward:/fallback/service-unavailable")))
                        .uri(serviceRoutes.authUrl()))
                .route("slot-lock", route -> route
                        .method(HttpMethod.POST)
                        .and()
                        .path("/slots/lock")
                        .filters(filters -> filters.requestRateLimiter(config -> config
                                .setRateLimiter(slotLockRateLimiter)
                                .setKeyResolver(userIdKeyResolver))
                                .circuitBreaker(config -> config
                                        .setName("bookingService")
                                        .setFallbackUri("forward:/fallback/service-unavailable")))
                        .uri(serviceRoutes.bookingUrl()))
                .route("payment-initiate", route -> route
                        .method(HttpMethod.POST)
                        .and()
                        .path("/payments/initiate")
                        .filters(filters -> filters.requestRateLimiter(config -> config
                                .setRateLimiter(paymentInitiateRateLimiter)
                                .setKeyResolver(userIdKeyResolver))
                                .circuitBreaker(config -> config
                                        .setName("paymentService")
                                        .setFallbackUri("forward:/fallback/service-unavailable")))
                        .uri(serviceRoutes.paymentUrl()))
                .route("payment-verify", route -> route
                        .method(HttpMethod.POST)
                        .and()
                        .path("/payments/verify")
                        .filters(filters -> filters.requestRateLimiter(config -> config
                                .setRateLimiter(paymentVerifyRateLimiter)
                                .setKeyResolver(userIdKeyResolver))
                                .circuitBreaker(config -> config
                                        .setName("paymentService")
                                        .setFallbackUri("forward:/fallback/service-unavailable")))
                        .uri(serviceRoutes.paymentUrl()))
                .route("payment-webhook", route -> route
                        .method(HttpMethod.POST)
                        .and()
                        .path("/payments/webhook")
                        .filters(filters -> filters.circuitBreaker(config -> config
                                .setName("paymentService")
                                .setFallbackUri("forward:/fallback/service-unavailable")))
                        .uri(serviceRoutes.paymentUrl()))
                .route("customer-service", route -> route
                        .path("/customerProfile/**", "/cars/**")
                        .filters(filters -> filters.requestRateLimiter(config -> config
                                .setRateLimiter(defaultUserRateLimiter)
                                .setKeyResolver(userIdKeyResolver))
                                .circuitBreaker(config -> config
                                        .setName("customerService")
                                        .setFallbackUri("forward:/fallback/service-unavailable")))
                        .uri(serviceRoutes.customerUrl()))
                .route("booking-service", route -> route
                        .path("/slots/**", "/bookings/**", "/admin/bookings/**")
                        .filters(filters -> filters.requestRateLimiter(config -> config
                                .setRateLimiter(defaultUserRateLimiter)
                                .setKeyResolver(userIdKeyResolver))
                                .circuitBreaker(config -> config
                                        .setName("bookingService")
                                        .setFallbackUri("forward:/fallback/service-unavailable")))
                        .uri(serviceRoutes.bookingUrl()))
                .route("payment-service", route -> route
                        .path("/payments/**", "/admin/payments/**")
                        .filters(filters -> filters.requestRateLimiter(config -> config
                                .setRateLimiter(defaultUserRateLimiter)
                                .setKeyResolver(userIdKeyResolver))
                                .circuitBreaker(config -> config
                                        .setName("paymentService")
                                        .setFallbackUri("forward:/fallback/service-unavailable")))
                        .uri(serviceRoutes.paymentUrl()))
                .route("washer-service", route -> route
                        .path("/washer/**")
                        .filters(filters -> filters.requestRateLimiter(config -> config
                                .setRateLimiter(defaultUserRateLimiter)
                                .setKeyResolver(userIdKeyResolver))
                                .circuitBreaker(config -> config
                                        .setName("washerService")
                                        .setFallbackUri("forward:/fallback/service-unavailable")))
                        .uri(serviceRoutes.washerUrl()))
                .build();
    }
}
