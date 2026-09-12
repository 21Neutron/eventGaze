package com.group9.eventgaze.utils;

import com.group9.eventgaze.entity.Colleges;
import com.group9.eventgaze.entity.Events;
import com.group9.eventgaze.entity.Publishers;
import com.group9.eventgaze.entity.dto.CollegeDTO;
import com.group9.eventgaze.entity.dto.EventResponseDTO;
import com.group9.eventgaze.entity.dto.PublisherDTO;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class EventMapper {

    public static EventResponseDTO toEventResponseDTO(Events event) {
        if (event == null) {
            return null;
        }
        EventResponseDTO dto = new EventResponseDTO();
        dto.setEventId(event.getEventId());
        dto.setEventName(event.getEventName());
        dto.setEventDescription(event.getEventDescription());
        dto.setEventDate(event.getEventDate() != null ? event.getEventDate().toString() : null);
        dto.setEventTags(event.getEventTags());
        dto.setEventArt(event.getEventArt());
        dto.setEventVenue(event.getEventVenue());
        dto.setCollege(toCollegeDTO(event.getCollege()));
        dto.setEventCategory(event.getEventCategory());
        dto.setPublishers(toPublisherDTO(event.getPublishers()));
        return dto;
    }

    private static CollegeDTO toCollegeDTO(Colleges college) {
        if (college == null) {
            return null;
        }
        CollegeDTO dto = new CollegeDTO();
        dto.setCollegeId(college.getCollegeId());
        dto.setCollegeName(college.getCollegeName());
        return dto;
    }

    private static PublisherDTO toPublisherDTO(Publishers publisher) {
        if (publisher == null) {
            return null;
        }
        PublisherDTO dto = new PublisherDTO();
        dto.setPublisherId(publisher.getPublisherId());
        dto.setPublisherOrgName(publisher.getPublisherOrgName());
        dto.setPublisherImage(publisher.getPublisherImage());
        return dto;
    }

    public static List<EventResponseDTO> toEventResponseDTOList(List<Events> events) {
        return events.stream()
                .map(EventMapper::toEventResponseDTO)
                .collect(Collectors.toList());
    }
}
