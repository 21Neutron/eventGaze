package com.group9.eventgaze.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "publishers")
public class Publishers {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "publisher_id")
    private Long publisherId;

    @Column(name = "publisher_org_name", nullable = false)
    private String publisherOrgName;

    @Column(name = "publisher_image")
    private String publisherImage;

    @Column(name = "publisher_description")
    private String publisherDescription;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "user_id", unique = true, nullable = false)
    private Users users;
}
