package com.example.backendjava.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

public class JwtUtil {
    public static String generateToken(Map<String, Object> claims, String secret, long expirationMillis) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        Date now = new Date();
        Date exp = new Date(now.getTime() + expirationMillis);
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public static Optional<Claims> parseToken(String token, String secret) {
        try {
            if (token == null || token.isBlank()) return Optional.empty();
            // Support "Bearer <token>" format
            String raw = token.startsWith("Bearer ") ? token.substring(7) : token;
            SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(raw)
                    .getBody();
            return Optional.ofNullable(claims);
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
