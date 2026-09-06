package com.group9.eventgaze.entity.dto;

import com.group9.eventgaze.entity.EventCategory;
import lombok.Data;

import java.util.List;

@Data
public class EventResponseDTO {
    private Long eventId;
    private String eventName;
    private String eventDescription;
    private String eventDate;
    private String eventTags;
    private String eventArt;
    private CollegeDTO college;
    private String eventVenue;
    private EventCategory eventCategory;
    private PublisherDTO publishers;
}
