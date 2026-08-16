package com.myspringproject.carwash.api_gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayRoutesConfig {

    @Bean
    public RouteLocator carWashRoutes(RouteLocatorBuilder builder, ServiceRouteProperties serviceRoutes) {
        return builder.routes()
                .route("auth-service", route -> route
                        .path("/auth/**")
                        .uri(serviceRoutes.authUrl()))
                .route("customer-service", route -> route
                        .path("/customerProfile/**", "/cars/**")
                        .uri(serviceRoutes.customerUrl()))
                .route("booking-service", route -> route
                        .path("/slots/**", "/bookings/**", "/admin/bookings/**")
                        .uri(serviceRoutes.bookingUrl()))
                .route("payment-service", route -> route
                        .path("/payments/**", "/admin/payments/**")
                        .uri(serviceRoutes.paymentUrl()))
                .route("washer-service", route -> route
                        .path("/washer/**")
                        .uri(serviceRoutes.washerUrl()))
                .build();
    }
}
