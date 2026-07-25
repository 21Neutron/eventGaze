package com.group9.eventgaze.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Data
@Entity
@Table(name = "events")
public class Events {

    @Id
    @Column(name = "event_id", nullable = false, unique = true)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long eventId;

    @Column(name = "event_name", nullable = false)
    private String eventName;

    @Column(name = "event_description", columnDefinition = "TEXT")
    private String eventDescription;

    @Column(name = "event_date")
    private LocalDate eventDate;

    @Column(name = "event_venue")
    private String eventVenue;

    @Column(name = "event_tags")
    private String eventTags;

    @Column(name = "event_art")
    private String eventArt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "publisher_id", referencedColumnName = "publisher_id")
    private Publishers publishers;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_category_id", referencedColumnName = "event_category_id")
    private EventCategory eventCategory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "college_id", referencedColumnName = "college_id")
    private Colleges college;
}
