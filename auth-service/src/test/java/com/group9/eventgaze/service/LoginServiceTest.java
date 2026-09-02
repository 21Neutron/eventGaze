package com.group9.eventgaze.service;

import com.group9.eventgaze.entity.Users;
import com.group9.eventgaze.entity.dto.LoginRequest;
import com.group9.eventgaze.entity.dto.LoginResponse;
import com.group9.eventgaze.repository.UsersRepo;
import com.group9.eventgaze.utils.JwtTokenUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginServiceTest {

    @Mock
    private UsersRepo usersRepo;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenUtil jwtTokenUtil;

    @InjectMocks
    private LoginService loginService;

    private Users user;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        user = new Users();
        user.setUserId(1L);
        user.setUserEmail("test@example.com");
        user.setUserPassword("encodedPassword");
        user.setUserRole("STUDENT");

        loginRequest = new LoginRequest();
        loginRequest.setUserName("test@example.com");
        loginRequest.setPassword("rawPassword");
    }

    @Test
    @DisplayName("logIn: Successful login returns token, role, and userId")
    void testLogIn_Success() {
        when(usersRepo.findByUserEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("rawPassword", "encodedPassword")).thenReturn(true);
        when(jwtTokenUtil.generateToken(1L, "STUDENT")).thenReturn("generatedJwtToken");

        LoginResponse response = loginService.logIn(loginRequest);

        assertNotNull(response);
        assertEquals("generatedJwtToken", response.getSessionToken());
        assertEquals("STUDENT", response.getUserRole());
        assertEquals(1L, response.getUserId());

        verify(usersRepo).findByUserEmail("test@example.com");
        verify(passwordEncoder).matches("rawPassword", "encodedPassword");
        verify(jwtTokenUtil).generateToken(1L, "STUDENT");
    }

    @Test
    @DisplayName("logIn: Throws 401 UNAUTHORIZED when user not found")
    void testLogIn_UserNotFound() {
        when(usersRepo.findByUserEmail("test@example.com")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                loginService.logIn(loginRequest));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(jwtTokenUtil, never()).generateToken(anyLong(), anyString());
    }

    @Test
    @DisplayName("logIn: Throws 401 UNAUTHORIZED when password does not match")
    void testLogIn_PasswordMismatch() {
        when(usersRepo.findByUserEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("rawPassword", "encodedPassword")).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                loginService.logIn(loginRequest));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        assertEquals("Invalid credentials", ex.getReason());
        verify(jwtTokenUtil, never()).generateToken(anyLong(), anyString());
    }
}
