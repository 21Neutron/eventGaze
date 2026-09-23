package com.group9.eventgaze.utils;

import com.group9.eventgaze.entity.Colleges;
import com.group9.eventgaze.entity.Events;
import com.group9.eventgaze.entity.Publishers;
import com.group9.eventgaze.entity.dto.CollegeDTO;
import com.group9.eventgaze.entity.dto.EventResponseDTO;
import com.group9.eventgaze.entity.dto.PublisherDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class EventMapper {

    private static StorageUrlResolver staticStorageUrlResolver = new StorageUrlResolver();
    private static String staticBaseUrl = "https://eventgazeeventimages.s3.amazonaws.com/";

    private final StorageUrlResolver storageUrlResolver;
    private final String baseUrl;

    @Autowired
    public EventMapper(
            StorageUrlResolver storageUrlResolver,
            @Value("${storage.base-url:https://eventgazeeventimages.s3.amazonaws.com/}") String baseUrl) {
        this.storageUrlResolver = storageUrlResolver != null ? storageUrlResolver : new StorageUrlResolver();
        this.baseUrl = (baseUrl != null && !baseUrl.isBlank()) ? baseUrl : "https://eventgazeeventimages.s3.amazonaws.com/";
        staticStorageUrlResolver = this.storageUrlResolver;
        staticBaseUrl = this.baseUrl;
    }

    public EventMapper() {
        this.storageUrlResolver = new StorageUrlResolver();
        this.baseUrl = "https://eventgazeeventimages.s3.amazonaws.com/";
    }

    public static void setStaticBaseUrl(String baseUrl) {
        staticBaseUrl = baseUrl;
    }

    public static void setStaticStorageUrlResolver(StorageUrlResolver resolver) {
        staticStorageUrlResolver = resolver;
    }

    public static String getStaticBaseUrl() {
        return staticBaseUrl;
    }

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

        StorageUrlResolver resolver = (staticStorageUrlResolver != null) ? staticStorageUrlResolver : new StorageUrlResolver();
        dto.setEventArt(resolver.resolveUrl(event.getEventArt(), staticBaseUrl));

        dto.setEventVenue(event.getEventVenue());
        dto.setCollege(toCollegeDTO(event.getCollege()));
        dto.setEventCategory(event.getEventCategory());
        dto.setPublishers(toPublisherDTO(event.getPublishers()));
        return dto;
    }

    public EventResponseDTO toDTO(Events event) {
        if (event == null) {
            return null;
        }
        EventResponseDTO dto = toEventResponseDTO(event);
        if (storageUrlResolver != null) {
            dto.setEventArt(storageUrlResolver.resolveUrl(event.getEventArt(), baseUrl));
        }
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
        if (events == null) {
            return List.of();
        }
        return events.stream()
                .map(EventMapper::toEventResponseDTO)
                .collect(Collectors.toList());
    }

    public List<EventResponseDTO> toDTOList(List<Events> events) {
        if (events == null) {
            return List.of();
        }
        return events.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
}
