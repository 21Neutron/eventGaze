package com.group9.eventgaze.service;



import com.group9.eventgaze.entity.Users;
import com.group9.eventgaze.entity.dto.CustomUserDetails;
import com.group9.eventgaze.repository.UsersRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service  // Register this service as a Spring Bean
public class CustomUserDetailsService implements UserDetailsService {


    @Autowired
    private UsersRepo userRepository; // Replace with your user repository

    @Override
    public UserDetails loadUserByUsername(String userEmail) throws UsernameNotFoundException {
        // Load user from the database using email or user ID
        Users users = userRepository.findByUserEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // Return CustomUserDetails
        return new CustomUserDetails(users.getUserId(), users.getUserRole());
    }
}

