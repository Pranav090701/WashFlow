package com.myspringproject.carwash.auth_service.util;

import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;

import jakarta.annotation.PostConstruct;


@Component
public class JwtUtil {

    //private static final String SECRET = "mysupersecurelongsecretkey1234567890";
    @Value("${jwt.secret}")
    private String SECRET;

    private Algorithm algorithm;
    private JWTVerifier verifier;

    @PostConstruct
    public void init() {
        System.out.println(SECRET + " : " + SECRET.length());
        algorithm = Algorithm.HMAC256(SECRET);
        verifier = JWT.require(algorithm).build();
    }

    public String generateToken(String userId, String email, String role) {
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
