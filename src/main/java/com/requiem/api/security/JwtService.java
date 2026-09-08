package com.requiem.api.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {
    @Value("${app.jwt.secret}")
    private String secret;

    // Generate token with payload { userId: id }, expires in 7 days.
    public String generateToken(String userId) {
        return Jwts.builder()
                .claim("userId", userId)
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plus(7, ChronoUnit.DAYS)))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    // Extract userId from token (throws JwtException if invalid)
    public String extractUserId(String token) {
        return Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("userId", String.class);
    }
}