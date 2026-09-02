package com.group9.eventgaze.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.group9.eventgaze.entity.dto.LoginRequest;
import com.group9.eventgaze.entity.dto.LoginResponse;
import com.group9.eventgaze.entity.dto.PublisherSignUpDTO;
import com.group9.eventgaze.entity.dto.StudentSignUpDTO;
import com.group9.eventgaze.service.LoginService;
import com.group9.eventgaze.service.SignUpService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class LogInSignUpControllerTest {

    @Mock
    private SignUpService userService;

    @Mock
    private LoginService loginService;

    @InjectMocks
    private LogInSignUpController controller;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("registerStudent: Returns 201 CREATED on success")
    void testRegisterStudent_Success() throws Exception {
        StudentSignUpDTO dto = new StudentSignUpDTO();
        dto.setUserEmail("student@test.com");
        dto.setUserPassword("pass123");
        dto.setFirstName("John");
        dto.setLastName("Doe");
        dto.setCollegeId(1L);

        doNothing().when(userService).registerStudent(any(StudentSignUpDTO.class));

        mockMvc.perform(post("/eventgaze/auth/student")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());

        verify(userService).registerStudent(any(StudentSignUpDTO.class));
    }

    @Test
    @DisplayName("registerStudent: Returns 400 BAD_REQUEST on exception")
    void testRegisterStudent_Failure() throws Exception {
        StudentSignUpDTO dto = new StudentSignUpDTO();
        doThrow(new IllegalArgumentException("Email is already registered"))
                .when(userService).registerStudent(any(StudentSignUpDTO.class));

        mockMvc.perform(post("/eventgaze/auth/student")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("registerPublisher: Returns 201 CREATED on success")
    void testRegisterPublisher_Success() throws Exception {
        PublisherSignUpDTO dto = new PublisherSignUpDTO();
        dto.setUserEmail("pub@test.com");
        dto.setUserPassword("pass123");
        dto.setPublisherOrgName("Org");

        doNothing().when(userService).registerPublisher(any(PublisherSignUpDTO.class));

        mockMvc.perform(post("/eventgaze/auth/publisher")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());

        verify(userService).registerPublisher(any(PublisherSignUpDTO.class));
    }

    @Test
    @DisplayName("registerPublisher: Returns 400 BAD_REQUEST on exception")
    void testRegisterPublisher_Failure() throws Exception {
        PublisherSignUpDTO dto = new PublisherSignUpDTO();
        doThrow(new IllegalArgumentException("Email is already registered"))
                .when(userService).registerPublisher(any(PublisherSignUpDTO.class));

        mockMvc.perform(post("/eventgaze/auth/publisher")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("login: Returns 200 OK with login response")
    void testLogin_Success() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setUserName("test@test.com");
        req.setPassword("password");

        LoginResponse resp = new LoginResponse("mock-jwt-token", "STUDENT", 42L);
        when(loginService.logIn(any(LoginRequest.class))).thenReturn(resp);

        mockMvc.perform(post("/eventgaze/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionToken").value("mock-jwt-token"))
                .andExpect(jsonPath("$.userRole").value("STUDENT"))
                .andExpect(jsonPath("$.userId").value(42));
    }

    @Test
    @DisplayName("login: Direct method invocation succeeds")
    void testLogin_DirectCall() {
        LoginRequest req = new LoginRequest();
        req.setUserName("test@test.com");
        req.setPassword("pass");

        LoginResponse resp = new LoginResponse("tok", "STUDENT", 1L);
        when(loginService.logIn(req)).thenReturn(resp);

        ResponseEntity<?> response = controller.login(req);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(resp, response.getBody());
    }
}
