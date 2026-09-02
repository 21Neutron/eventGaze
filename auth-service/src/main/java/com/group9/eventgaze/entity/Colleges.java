package com.group9.eventgaze.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "college_list")
public class Colleges {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "college_id")
    private Long collegeId;

    @Column(name = "college_name")
    private String collegeName;

    @Column(name = "college_address")
    private String collegeAddress;

    @Column(name = "college_image")
    private String collegeImage;
}
