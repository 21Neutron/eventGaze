package com.group9.eventgaze.controller;

import com.group9.eventgaze.entity.Events;
import com.group9.eventgaze.service.EventsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventsPaginationTest {

    @Mock
    private EventsService eventsService;

    @InjectMocks
    private EventsController eventsController;

    @Test
    void testGetEvents_WithPagination() {
        Events event = new Events();
        event.setEventId(1L);
        event.setEventName("Hackathon 2026");

        Page<Events> pagedEvents = new PageImpl<>(List.of(event), PageRequest.of(0, 10), 1);
        when(eventsService.getEvents(any(Pageable.class))).thenReturn(pagedEvents);

        ResponseEntity<?> response = eventsController.getEvents(0, 10, "eventId", "ASC");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof Page<?>);
        Page<?> body = (Page<?>) response.getBody();
        assertEquals(1, body.getTotalElements());
        verify(eventsService).getEvents(any(Pageable.class));
    }

    @Test
    void testGetEventsByCategory_WithPagination() {
        Events event = new Events();
        event.setEventId(2L);
        event.setEventName("Music Fest");

        Page<Events> pagedEvents = new PageImpl<>(List.of(event), PageRequest.of(0, 5), 1);
        when(eventsService.getEventsByCategory(eq(1L), any(Pageable.class))).thenReturn(pagedEvents);

        ResponseEntity<?> response = eventsController.getEventsByCategory(1L, 0, 5, "eventId", "DESC");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof Page<?>);
        Page<?> body = (Page<?>) response.getBody();
        assertEquals(1, body.getTotalElements());
        verify(eventsService).getEventsByCategory(eq(1L), any(Pageable.class));
    }

    @Test
    void testGetEventsByCollege_WithPagination() {
        Events event = new Events();
        event.setEventId(3L);
        event.setEventName("Tech Symposium");

        Page<Events> pagedEvents = new PageImpl<>(List.of(event), PageRequest.of(0, 5), 1);
        when(eventsService.getEventsByCollegeId(eq(10L), any(Pageable.class))).thenReturn(pagedEvents);

        ResponseEntity<?> response = eventsController.getEventsByCollegeId(10L, 0, 5, "eventId", "ASC");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof Page<?>);
        Page<?> body = (Page<?>) response.getBody();
        assertEquals(1, body.getTotalElements());
        verify(eventsService).getEventsByCollegeId(eq(10L), any(Pageable.class));
    }

    @Test
    void testSearchEvents_WithPagination() {
        Events event = new Events();
        event.setEventId(4L);
        event.setEventName("AI Summit");
        event.setEventDescription("All about AI and ML");
        event.setEventTags("tech,ai");

        Page<Events> pagedEvents = new PageImpl<>(List.of(event), PageRequest.of(0, 10), 1);
        when(eventsService.searchEvents(eq("AI"), any(Pageable.class))).thenReturn(pagedEvents);

        ResponseEntity<?> response = eventsController.searchEvents("AI", 0, 10, "eventId", "ASC");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof Page<?>);
        Page<?> body = (Page<?>) response.getBody();
        assertEquals(1, body.getTotalElements());
        verify(eventsService).searchEvents(eq("AI"), any(Pageable.class));
    }

    @Test
    void testGetUpcomingEvents_WithPagination() {
        Events event = new Events();
        event.setEventId(5L);
        event.setEventName("Future Hackathon");

        Page<Events> pagedEvents = new PageImpl<>(List.of(event), PageRequest.of(0, 10), 1);
        when(eventsService.getUpcomingEvents(any(Pageable.class))).thenReturn(pagedEvents);

        ResponseEntity<?> response = eventsController.getUpcomingEvents(0, 10, "eventId", "ASC");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof Page<?>);
        Page<?> body = (Page<?>) response.getBody();
        assertEquals(1, body.getTotalElements());
        verify(eventsService).getUpcomingEvents(any(Pageable.class));
    }

    @Test
    void testGetPastEvents_WithPagination() {
        Events event = new Events();
        event.setEventId(6L);
        event.setEventName("Past Graduation Ceremony");

        Page<Events> pagedEvents = new PageImpl<>(List.of(event), PageRequest.of(0, 10), 1);
        when(eventsService.getPastEvents(any(Pageable.class))).thenReturn(pagedEvents);

        ResponseEntity<?> response = eventsController.getPastEvents(0, 10, "eventId", "DESC");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof Page<?>);
        Page<?> body = (Page<?>) response.getBody();
        assertEquals(1, body.getTotalElements());
        verify(eventsService).getPastEvents(any(Pageable.class));
    }
}
