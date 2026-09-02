package com.group9.eventgaze.entity.dto;

import lombok.Data;

@Data
public class PublisherSignUpDTO {
    private String userEmail;
    private String userPassword;
    private String publisherOrgName;
}
