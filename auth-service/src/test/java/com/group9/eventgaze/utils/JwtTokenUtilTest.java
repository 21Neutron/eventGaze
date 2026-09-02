package com.group9.eventgaze.utils;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenUtilTest {

    private JwtTokenUtil jwtTokenUtil;
    private final String secretKey = "testjwtsecretkeywithsufficientlengthforvalidation1234567890";
    private final long expirationTime = 86400000L; // 24 hours

    @BeforeEach
    void setUp() {
        jwtTokenUtil = new JwtTokenUtil(secretKey, expirationTime);
    }

    @Test
    @DisplayName("generateToken: Creates a valid JWT with subject and role")
    void testGenerateToken() {
        String token = jwtTokenUtil.generateToken(42L, "STUDENT");
        assertNotNull(token);
        assertTrue(jwtTokenUtil.isTokenValid(token));

        Claims claims = jwtTokenUtil.getClaimsFromToken(token);
        assertEquals("42", claims.getSubject());
        assertEquals("STUDENT", claims.get("role", String.class));
        assertNotNull(claims.getExpiration());
        assertNotNull(claims.getIssuedAt());
    }

    @Test
    @DisplayName("isTokenValid: Handles valid, invalid, expired, and malformed tokens")
    void testIsTokenValid() {
        String validToken = jwtTokenUtil.generateToken(1L, "PUBLISHER");
        assertTrue(jwtTokenUtil.isTokenValid(validToken));
        assertTrue(jwtTokenUtil.isTokenValid("Bearer " + validToken));

        assertFalse(jwtTokenUtil.isTokenValid(null));
        assertFalse(jwtTokenUtil.isTokenValid(""));
        assertFalse(jwtTokenUtil.isTokenValid("   "));
        assertFalse(jwtTokenUtil.isTokenValid("invalid.jwt.token"));
        assertFalse(jwtTokenUtil.isTokenValid(validToken + "tampered"));

        // Expired token test
        JwtTokenUtil shortLivedUtil = new JwtTokenUtil(secretKey, -1000L); // already expired
        String expiredToken = shortLivedUtil.generateToken(2L, "STUDENT");
        assertFalse(shortLivedUtil.isTokenValid(expiredToken));
    }

    @Test
    @DisplayName("refreshToken: Successfully refreshes a valid token")
    void testRefreshToken_Success() {
        String originalToken = jwtTokenUtil.generateToken(99L, "PUBLISHER");
        assertNotNull(originalToken);

        String refreshedToken = jwtTokenUtil.refreshToken(originalToken);
        assertNotNull(refreshedToken);
        assertTrue(jwtTokenUtil.isTokenValid(refreshedToken));

        Claims claims = jwtTokenUtil.getClaimsFromToken(refreshedToken);
        assertEquals("99", claims.getSubject());
        assertEquals("PUBLISHER", claims.get("role", String.class));
    }

    @Test
    @DisplayName("refreshToken: Successfully refreshes a token with Bearer prefix")
    void testRefreshToken_WithBearerPrefix() {
        String originalToken = jwtTokenUtil.generateToken(88L, "STUDENT");
        String bearerToken = "Bearer " + originalToken;

        String refreshedToken = jwtTokenUtil.refreshToken(bearerToken);
        assertNotNull(refreshedToken);
        assertTrue(jwtTokenUtil.isTokenValid(refreshedToken));

        Claims claims = jwtTokenUtil.getClaimsFromToken(refreshedToken);
        assertEquals("88", claims.getSubject());
        assertEquals("STUDENT", claims.get("role", String.class));
    }

    @Test
    @DisplayName("refreshToken: Throws exception when refreshing invalid, null, or blank token")
    void testRefreshToken_InvalidToken() {
        assertThrows(IllegalArgumentException.class, () -> jwtTokenUtil.refreshToken(null));
        assertThrows(IllegalArgumentException.class, () -> jwtTokenUtil.refreshToken(""));
        assertThrows(IllegalArgumentException.class, () -> jwtTokenUtil.refreshToken("   "));
        assertThrows(IllegalArgumentException.class, () -> jwtTokenUtil.refreshToken("invalid.token.here"));

        JwtTokenUtil shortLivedUtil = new JwtTokenUtil(secretKey, -1000L);
        String expiredToken = shortLivedUtil.generateToken(5L, "STUDENT");
        assertThrows(IllegalArgumentException.class, () -> shortLivedUtil.refreshToken(expiredToken));
    }

    @Test
    @DisplayName("getters and setters work as expected")
    void testGettersAndSetters() {
        JwtTokenUtil util = new JwtTokenUtil();
        util.setKey("mykey");
        util.setExpirationTime(3600000L);

        assertEquals("mykey", util.getKey());
        assertEquals(3600000L, util.getExpirationTime());
    }
}
