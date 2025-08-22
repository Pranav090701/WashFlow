package com.myspringproject.carwash.auth_service.util;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;

import jakarta.annotation.PostConstruct;


@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    private Algorithm algorithm;
    private JWTVerifier verifier;

    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

    @PostConstruct
    public void init() {
        algorithm = Algorithm.HMAC256(secret);
        verifier = JWT.require(algorithm).build();
    }

    public String generateToken(String userId, String email, String role) {
        logger.info("Generating JWT for userId: {}", userId);
        Date issuedAt = new Date();
        Date expiresAt = new Date(issuedAt.getTime() + (3L * 24 * 60 * 60 * 1000)); // 3 days

        return JWT.create()
                .withSubject(userId)
                .withClaim("email", email)
                .withClaim("role", role)
                .withIssuedAt(issuedAt)
                .withExpiresAt(expiresAt)
                .sign(algorithm);
    }

    public DecodedJWT decodeToken(String token) {
        return verifier.verify(token);
    }

    public String extractUserId(String token) {
        return decodeToken(token).getSubject();
    }
}
