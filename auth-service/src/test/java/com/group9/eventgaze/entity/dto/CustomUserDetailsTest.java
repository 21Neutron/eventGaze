package com.group9.eventgaze.entity.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

class CustomUserDetailsTest {

    @Test
    @DisplayName("getAuthorities: Adds ROLE_ prefix when not present")
    void testAuthoritiesWithRoleWithoutPrefix() {
        CustomUserDetails userDetails = new CustomUserDetails(1L, "PUBLISHER");
        Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();

        assertNotNull(authorities);
        assertEquals(1, authorities.size());
        assertEquals("ROLE_PUBLISHER", authorities.iterator().next().getAuthority());
    }

    @Test
    @DisplayName("getAuthorities: Preserves ROLE_ prefix when already present")
    void testAuthoritiesWithRoleAlreadyPrefixed() {
        CustomUserDetails userDetails = new CustomUserDetails(2L, "ROLE_STUDENT");
        Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();

        assertNotNull(authorities);
        assertEquals(1, authorities.size());
        assertEquals("ROLE_STUDENT", authorities.iterator().next().getAuthority());
    }

    @Test
    @DisplayName("getAuthorities: Returns empty list for null or blank role")
    void testAuthoritiesWithNullOrBlankRole() {
        CustomUserDetails userDetailsNull = new CustomUserDetails(3L, null);
        assertTrue(userDetailsNull.getAuthorities().isEmpty());

        CustomUserDetails userDetailsBlank = new CustomUserDetails(4L, "  ");
        assertTrue(userDetailsBlank.getAuthorities().isEmpty());
    }

    @Test
    @DisplayName("UserDetails interface contract methods return defaults")
    void testUserDetailsInterfaceDefaults() {
        CustomUserDetails userDetails = new CustomUserDetails(10L, "STUDENT");
        assertEquals("10", userDetails.getUsername());
        assertNull(userDetails.getPassword());
        assertTrue(userDetails.isAccountNonExpired());
        assertTrue(userDetails.isAccountNonLocked());
        assertTrue(userDetails.isCredentialsNonExpired());
        assertTrue(userDetails.isEnabled());
    }
}
