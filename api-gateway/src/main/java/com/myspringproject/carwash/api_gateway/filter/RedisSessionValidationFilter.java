package com.myspringproject.carwash.api_gateway.filter;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import reactor.core.publisher.Mono;

public class RedisSessionValidationFilter implements WebFilter {

    private static final Logger logger = LoggerFactory.getLogger(RedisSessionValidationFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";

    private final ReactiveStringRedisTemplate redisTemplate;
    private final String sessionPrefix;
    private final Duration sessionTtl;

    public RedisSessionValidationFilter(
            ReactiveStringRedisTemplate redisTemplate,
            String sessionPrefix,
            long sessionTtlSeconds) {
        this.redisTemplate = redisTemplate;
        this.sessionPrefix = sessionPrefix;
        this.sessionTtl = Duration.ofSeconds(sessionTtlSeconds);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (shouldSkip(exchange)) {
            return chain.filter(exchange);
        }

        return authenticatedJwt(exchange)
                .switchIfEmpty(Mono.defer(() -> unauthorized(exchange)))
                .flatMap(authentication -> validateSession(exchange, chain, authentication));
    }

    private Mono<Void> validateSession(
            ServerWebExchange exchange,
            WebFilterChain chain,
            JwtAuthenticationToken authentication) {
        Jwt jwt = authentication.getToken();
        String token = extractBearerToken(exchange);

        if (jwt.getSubject() == null || jwt.getSubject().isBlank() || token == null) {
            return unauthorized(exchange);
        }

        String key = sessionPrefix + jwt.getSubject();
        return redisTemplate.opsForValue().get(key)
                .flatMap(storedToken -> {
                    if (!token.equals(storedToken)) {
                        return unauthorized(exchange);
                    }
                    return redisTemplate.expire(key, sessionTtl)
                            .then(chain.filter(exchange));
                })
                .switchIfEmpty(Mono.defer(() -> unauthorized(exchange)))
                .onErrorResume(ex -> {
                    logger.error("Unable to validate Redis session for user {}", jwt.getSubject(), ex);
                    exchange.getResponse().setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
                    return exchange.getResponse().setComplete();
                });
    }

    private Mono<JwtAuthenticationToken> authenticatedJwt(ServerWebExchange exchange) {
        Mono<JwtAuthenticationToken> fromSecurityContext = ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .filter(JwtAuthenticationToken.class::isInstance)
                .cast(JwtAuthenticationToken.class);

        Mono<JwtAuthenticationToken> fromExchangePrincipal = exchange.getPrincipal()
                .filter(Authentication.class::isInstance)
                .cast(Authentication.class)
                .filter(JwtAuthenticationToken.class::isInstance)
                .cast(JwtAuthenticationToken.class);

        return fromSecurityContext.switchIfEmpty(fromExchangePrincipal);
    }

    private boolean shouldSkip(ServerWebExchange exchange) {
        String path = exchange.getRequest().getPath().pathWithinApplication().value();
        HttpMethod method = exchange.getRequest().getMethod();

        return HttpMethod.OPTIONS.equals(method)
                || path.equals("/actuator/health")
                || path.equals("/actuator/info")
                || path.startsWith("/fallback/")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.equals("/auth/verify") && HttpMethod.GET.equals(method)
                || path.equals("/auth/register") && HttpMethod.POST.equals(method)
                || path.equals("/auth/login") && HttpMethod.POST.equals(method)
                || path.equals("/auth/resend-verification") && HttpMethod.POST.equals(method)
                || path.equals("/payments/webhook") && HttpMethod.POST.equals(method);
    }

    private String extractBearerToken(ServerWebExchange exchange) {
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            return null;
        }
        return authHeader.substring(BEARER_PREFIX.length());
    }

    private <T> Mono<T> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete().then(Mono.empty());
    }
}
