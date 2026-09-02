package com.group9.eventgaze.service;

import com.group9.eventgaze.entity.Users;
import com.group9.eventgaze.entity.dto.LoginRequest;
import com.group9.eventgaze.entity.dto.LoginResponse;
import com.group9.eventgaze.repository.UsersRepo;
import com.group9.eventgaze.utils.JwtTokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LoginService {

    @Autowired
    private UsersRepo usersRepo;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    public LoginService() {
    }

    public LoginService(UsersRepo usersRepo, BCryptPasswordEncoder passwordEncoder, JwtTokenUtil jwtTokenUtil) {
        this.usersRepo = usersRepo;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenUtil = jwtTokenUtil;
    }

    public LoginResponse logIn(LoginRequest loginRequest) {
        // Retrieve the user by email
        Users users = usersRepo.findByUserEmail(loginRequest.getUserName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        // Validate password
        if (!passwordEncoder.matches(loginRequest.getPassword(), users.getUserPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        // Generate JWT token
        String jwtToken = jwtTokenUtil.generateToken(users.getUserId(), users.getUserRole());

        return new LoginResponse(jwtToken, users.getUserRole(), users.getUserId());
    }
}
