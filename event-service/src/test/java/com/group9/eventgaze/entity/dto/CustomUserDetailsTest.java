package com.group9.eventgaze.entity.dto;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

class CustomUserDetailsTest {

    @Test
    void testAuthoritiesWithRoleWithoutPrefix() {
        CustomUserDetails userDetails = new CustomUserDetails(1L, "PUBLISHER");
        Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();

        assertNotNull(authorities);
        assertEquals(1, authorities.size());
        assertEquals("ROLE_PUBLISHER", authorities.iterator().next().getAuthority());
    }

    @Test
    void testAuthoritiesWithRoleAlreadyPrefixed() {
        CustomUserDetails userDetails = new CustomUserDetails(2L, "ROLE_STUDENT");
        Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();

        assertNotNull(authorities);
        assertEquals(1, authorities.size());
        assertEquals("ROLE_STUDENT", authorities.iterator().next().getAuthority());
    }

    @Test
    void testAuthoritiesWithNullOrBlankRole() {
        CustomUserDetails userDetailsNull = new CustomUserDetails(3L, null);
        assertTrue(userDetailsNull.getAuthorities().isEmpty());

        CustomUserDetails userDetailsBlank = new CustomUserDetails(4L, "  ");
        assertTrue(userDetailsBlank.getAuthorities().isEmpty());
    }
}
