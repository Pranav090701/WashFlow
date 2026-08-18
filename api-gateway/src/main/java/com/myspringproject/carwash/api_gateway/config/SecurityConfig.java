package com.myspringproject.carwash.api_gateway.config;

import java.util.List;

import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;

import com.myspringproject.carwash.api_gateway.filter.RedisSessionValidationFilter;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity http,
            RedisSessionValidationFilter redisSessionValidationFilter) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(Customizer.withDefaults())
                .authorizeExchange(auth -> auth
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .pathMatchers("/actuator/health", "/actuator/info").permitAll()
                        .pathMatchers("/fallback/**").permitAll()
                        .pathMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()

                        .pathMatchers(HttpMethod.POST, "/auth/register", "/auth/login", "/auth/resend-verification").permitAll()
                        .pathMatchers(HttpMethod.GET, "/auth/verify").permitAll()
                        .pathMatchers("/auth/**").authenticated()

                        .pathMatchers(HttpMethod.GET, "/washer/available").denyAll()
                        .pathMatchers(HttpMethod.GET, "/bookings/*/validate").denyAll()

                        .pathMatchers("/admin/bookings/**", "/admin/payments/**").hasRole("ADMIN")

                        .pathMatchers("/customerProfile/**").hasRole("CUSTOMER")
                        .pathMatchers("/cars/**").hasRole("CUSTOMER")

                        .pathMatchers(HttpMethod.GET, "/slots/available").hasAnyRole("CUSTOMER", "WASHER", "ADMIN")
                        .pathMatchers(HttpMethod.POST, "/slots/lock").hasRole("CUSTOMER")
                        .pathMatchers(HttpMethod.POST, "/slots/locked-quote").denyAll()
                        .pathMatchers(HttpMethod.POST, "/slots/confirm").denyAll()
                        .pathMatchers(HttpMethod.POST, "/slots/generate").hasRole("ADMIN")
                        .pathMatchers(HttpMethod.PATCH, "/bookings/*/complete").hasAnyRole("WASHER", "ADMIN")

                        .pathMatchers(HttpMethod.POST, "/payments/webhook").permitAll()
                        .pathMatchers(HttpMethod.POST, "/payments/initiate", "/payments/verify").hasRole("CUSTOMER")
                        .pathMatchers(HttpMethod.GET, "/payments/**").hasAnyRole("CUSTOMER", "ADMIN")

                        .pathMatchers(HttpMethod.POST, "/washer/*/ratings").hasRole("CUSTOMER")
                        .pathMatchers(HttpMethod.GET, "/washer/*/ratings", "/washer/*/averageRatings")
                        .hasAnyRole("CUSTOMER", "ADMIN")
                        .pathMatchers(HttpMethod.POST, "/washer").hasAnyRole("WASHER", "ADMIN")
                        .pathMatchers(HttpMethod.PUT, "/washer").hasRole("WASHER")
                        .pathMatchers(HttpMethod.PATCH, "/washer/availability").hasAnyRole("WASHER", "ADMIN")
                        .pathMatchers(HttpMethod.GET, "/washer/by-area").hasAnyRole("CUSTOMER", "ADMIN")
                        .pathMatchers(HttpMethod.GET, "/washer/*").hasAnyRole("CUSTOMER", "WASHER", "ADMIN")

                        .anyExchange().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
                .addFilterAfter(redisSessionValidationFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .build();
    }

    @Bean
    public Converter<Jwt, Mono<AbstractAuthenticationToken>> jwtAuthenticationConverter() {
        ReactiveJwtAuthenticationConverter converter = new ReactiveJwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            String role = jwt.getClaimAsString("role");
            if (role == null || role.isBlank()) {
                return Flux.empty();
            }
            return Flux.fromIterable(List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        });
        return converter;
    }

    @Bean
    public ReactiveJwtDecoder jwtDecoder() {
        SecretKeySpec secretKey = new SecretKeySpec(jwtSecret.getBytes(), "HmacSHA256");
        return NimbusReactiveJwtDecoder.withSecretKey(secretKey).build();
    }

    @Bean
    public RedisSessionValidationFilter redisSessionValidationFilter(
            ReactiveStringRedisTemplate redisTemplate,
            @Value("${carwash.security.session-prefix:session:}") String sessionPrefix,
            @Value("${carwash.security.session-ttl-seconds:1200}") long sessionTtlSeconds) {
        return new RedisSessionValidationFilter(redisTemplate, sessionPrefix, sessionTtlSeconds);
    }
}
