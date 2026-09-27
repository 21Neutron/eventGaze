package com.group9.eventgaze.entity.dto;

import com.group9.eventgaze.entity.EventCategory;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class DtoTest {

    @Test
    void testCollegeDTO() {
        CollegeDTO college = CollegeDTO.builder()
                .collegeId(1L)
                .collegeName("Test College")
                .build();
        assertEquals(1L, college.getCollegeId());
        assertEquals("Test College", college.getCollegeName());
    }

    @Test
    void testPublisherDTO() {
        PublisherDTO publisher = PublisherDTO.builder()
                .publisherId(2L)
                .publisherOrgName("Org Name")
                .publisherImage("http://img.url")
                .build();
        assertEquals(2L, publisher.getPublisherId());
        assertEquals("Org Name", publisher.getPublisherOrgName());
        assertEquals("http://img.url", publisher.getPublisherImage());
    }

    @Test
    void testEventResponseDTO() {
        CollegeDTO college = new CollegeDTO(1L, "MIT");
        PublisherDTO publisher = new PublisherDTO(2L, "Club", "img.jpg");
        EventCategory category = new EventCategory(3L, "Tech");

        EventResponseDTO event = EventResponseDTO.builder()
                .eventId(10L)
                .eventName("Hackathon")
                .eventDescription("Coding 24h")
                .eventDate("2026-12-01")
                .eventTags("code,ai")
                .eventArt("art.png")
                .eventArtThumb("art-thumb.png")
                .college(college)
                .eventVenue("Hall A")
                .eventCategory(category)
                .publishers(publisher)
                .build();

        assertEquals(10L, event.getEventId());
        assertEquals("Hackathon", event.getEventName());
        assertEquals("art.png", event.getEventArt());
        assertEquals("art-thumb.png", event.getEventArtThumb());
        assertEquals(college, event.getCollege());
        assertEquals(publisher, event.getPublishers());
        assertEquals(category, event.getEventCategory());
    }

    @Test
    void testUploadResponseDTO() {
        UploadResponseDTO upload = UploadResponseDTO.builder()
                .fileUrl("https://s3.amazonaws.com/events/uuid.jpg")
                .key("events/uuid.jpg")
                .fileSize(1024L)
                .contentType("image/jpeg")
                .thumbnailKey("events/uuid-thumb.jpg")
                .thumbnailUrl("https://s3.amazonaws.com/events/uuid-thumb.jpg")
                .width(1280)
                .height(720)
                .thumbWidth(400)
                .thumbHeight(225)
                .thumbFileSize(512L)
                .build();

        assertEquals("https://s3.amazonaws.com/events/uuid.jpg", upload.getFileUrl());
        assertEquals("events/uuid.jpg", upload.getKey());
        assertEquals(1024L, upload.getFileSize());
        assertEquals("image/jpeg", upload.getContentType());
        assertEquals("events/uuid-thumb.jpg", upload.getThumbnailKey());
        assertEquals("https://s3.amazonaws.com/events/uuid-thumb.jpg", upload.getThumbnailUrl());
        assertEquals(1280, upload.getWidth());
        assertEquals(720, upload.getHeight());
        assertEquals(400, upload.getThumbWidth());
        assertEquals(225, upload.getThumbHeight());
        assertEquals(512L, upload.getThumbFileSize());
    }

    @Test
    void testErrorResponse() {
        LocalDateTime now = LocalDateTime.now();
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(now)
                .status(400)
                .error("Bad Request")
                .message("Invalid input")
                .path("/api/test")
                .build();

        assertEquals(400, error.getStatus());
        assertEquals("Bad Request", error.getError());
        assertEquals("Invalid input", error.getMessage());
        assertEquals("/api/test", error.getPath());
        assertEquals(now, error.getTimestamp());
    }
}
