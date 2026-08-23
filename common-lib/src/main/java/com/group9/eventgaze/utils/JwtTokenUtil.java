package com.group9.eventgaze.utils;

import io.jsonwebtoken.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class JwtTokenUtil {

    @Value("${JWT.SECRET.KEY:${jwt.secret.key:defaultjwtsecretkeywithsufficientlengthforvalidation1234567890}}")
    private String key;

    @Value("${jwt.expiration.ms:${JWT.EXPIRATION.TIME:86400000}}")
    private long expirationTime = 86400000L; // 24 hours default in milliseconds

    public JwtTokenUtil() {
    }

    public JwtTokenUtil(String key, long expirationTime) {
        this.key = key;
        this.expirationTime = expirationTime;
    }

    public String generateToken(Long userId, String role) {
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(SignatureAlgorithm.HS256, key)
                .compact();
    }

    public Claims getClaimsFromToken(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        return Jwts.parser()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean isTokenValid(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        try {
            getClaimsFromToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String refreshToken(String token) {
        if (!isTokenValid(token)) {
            throw new IllegalArgumentException("Cannot refresh invalid or expired token");
        }
        Claims claims = getClaimsFromToken(token);
        Long userId = Long.parseLong(claims.getSubject());
        String role = claims.get("role", String.class);
        return generateToken(userId, role);
    }

    public long getExpirationTime() {
        return expirationTime;
    }

    public void setExpirationTime(long expirationTime) {
        this.expirationTime = expirationTime;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}
