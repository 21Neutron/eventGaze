package com.group9.eventgaze.controller;

import com.group9.eventgaze.entity.dto.LoginRequest;
import com.group9.eventgaze.entity.dto.LoginResponse;
import com.group9.eventgaze.entity.dto.PublisherSignUpDTO;
import com.group9.eventgaze.entity.dto.StudentSignUpDTO;
import com.group9.eventgaze.service.LoginService;
import com.group9.eventgaze.service.SignUpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("eventgaze/auth")
public class LogInSignUpController {

    @Autowired
    private SignUpService userService;

    @Autowired
    private LoginService loginService;

    public LogInSignUpController() {
    }

    public LogInSignUpController(SignUpService userService, LoginService loginService) {
        this.userService = userService;
        this.loginService = loginService;
    }

    // creating an account for student
    @PostMapping("/student")
    public ResponseEntity<?> registerStudent(@RequestBody StudentSignUpDTO studentDTO) {
        try {
            userService.registerStudent(studentDTO);
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    // creating an account for publisher
    @PostMapping("/publisher")
    public ResponseEntity<?> registerPublisher(@RequestBody PublisherSignUpDTO publisherDTO) {
        try {
            userService.registerPublisher(publisherDTO);
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        LoginResponse loginResponse = loginService.logIn(loginRequest);
        return ResponseEntity.ok(loginResponse);
    }
}
