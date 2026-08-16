package com.myspringproject.carwash.api_gateway.filter;

import java.util.Locale;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

@Component
public class IdentityHeaderFilter implements GlobalFilter, Ordered {

    private static final String X_USER_ID = "X-User-Id";
    private static final String X_CUSTOMER_ID = "X-Customer-Id";
    private static final String X_WASHER_ID = "X-Washer-Id";
    private static final String X_USER_ROLE = "X-User-Role";
    private static final String X_USER_EMAIL = "X-User-Email";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return exchange.getPrincipal()
                .filter(JwtAuthenticationToken.class::isInstance)
                .cast(JwtAuthenticationToken.class)
                .map(JwtAuthenticationToken::getToken)
                .map(jwt -> withTrustedIdentityHeaders(exchange, jwt))
                .defaultIfEmpty(withTrustedIdentityHeaders(exchange, null))
                .flatMap(chain::filter);
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    private ServerWebExchange withTrustedIdentityHeaders(ServerWebExchange exchange, Jwt jwt) {
        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.remove(X_USER_ID);
                    headers.remove(X_CUSTOMER_ID);
                    headers.remove(X_WASHER_ID);
                    headers.remove(X_USER_ROLE);
                    headers.remove(X_USER_EMAIL);

                    if (jwt == null) {
                        return;
                    }

                    String userId = jwt.getSubject();
                    String role = jwt.getClaimAsString("role");
                    String email = jwt.getClaimAsString("email");

                    if (userId != null && !userId.isBlank()) {
                        headers.set(X_USER_ID, userId);
                    }
                    if (role != null && !role.isBlank()) {
                        headers.set(X_USER_ROLE, role);
                        if ("CUSTOMER".equals(role.toUpperCase(Locale.ROOT))) {
                            headers.set(X_CUSTOMER_ID, userId);
                        }
                        if ("WASHER".equals(role.toUpperCase(Locale.ROOT))) {
                            headers.set(X_WASHER_ID, userId);
                        }
                    }
                    if (email != null && !email.isBlank()) {
                        headers.set(X_USER_EMAIL, email);
                    }
                })
                .build();

        return exchange.mutate().request(request).build();
    }
}
