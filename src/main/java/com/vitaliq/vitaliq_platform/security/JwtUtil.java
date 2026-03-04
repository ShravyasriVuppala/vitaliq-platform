package com.vitaliq.vitaliq_platform.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HexFormat;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final JwtConfig jwtConfig;

    // Converts hex secret from config into a cryptographic signing key
    private SecretKey getSigningKey() {
        byte[] keyBytes = HexFormat.of().parseHex(jwtConfig.getSecret());
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // Generates a short-lived access token (15 mins)
    public String generateAccessToken(UUID userId, String email) {
        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("type", "access")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtConfig.getAccessTokenExpiration()))
                .signWith(getSigningKey())
                .compact();
    }

    // Generates a long-lived refresh token (7 days)
    public String generateRefreshToken(UUID userId) {
        return Jwts.builder()
                .subject(userId.toString())
                .claim("type", "refresh")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtConfig.getRefreshTokenExpiration()))
                .signWith(getSigningKey())
                .compact();
    }

    // Validates token signature and expiry — returns true if valid
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // Extracts all claims (payload) from a token
    private Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // Extracts userId from token subject
    public UUID extractUserId(String token) {
        return UUID.fromString(extractClaims(token).getSubject());
    }

    // Extracts email from token claims
    public String extractEmail(String token) {
        return extractClaims(token).get("email", String.class);
    }

    // Extracts token type — "access" or "refresh"
    public String extractTokenType(String token) {
        return extractClaims(token).get("type", String.class);
    }
}