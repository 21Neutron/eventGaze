package com.group9.eventgaze.entity.dto;

import lombok.Data;

@Data
public class StudentSignUpDTO {
    private String userEmail;
    private String userPassword;
    private String firstName;
    private String lastName;
    private Long collegeId;
}
