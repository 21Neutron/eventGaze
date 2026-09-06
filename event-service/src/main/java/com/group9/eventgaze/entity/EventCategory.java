package com.group9.eventgaze.entity;


import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "event_category")
public class EventCategory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_category_id")
    private Long categoryId;

    @Column(name = "category_name")
    private String categoryName;

}
