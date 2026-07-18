package com.group9.eventgaze.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SecurityConfigTest {

    @Test
    @DisplayName("corsConfigurationSource: Produces valid CorsConfiguration with required methods, headers, and origins")
    void testCorsConfigurationSource() {
        SecurityConfig config = new SecurityConfig(null);
        CorsConfigurationSource source = config.corsConfigurationSource();

        assertNotNull(source);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/eventgaze/events/getAll");
        CorsConfiguration corsConfig = source.getCorsConfiguration(request);

        assertNotNull(corsConfig);
        assertEquals(List.of("*"), corsConfig.getAllowedOrigins());
        assertTrue(corsConfig.getAllowedMethods().containsAll(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")));
        assertEquals(List.of("*"), corsConfig.getAllowedHeaders());
    }

    @Test
    @DisplayName("bCryptPasswordEncoder bean is instantiated")
    void testPasswordEncoderBean() {
        SecurityConfig config = new SecurityConfig(null);
        assertNotNull(config.bCryptPasswordEncoder());
    }
}
