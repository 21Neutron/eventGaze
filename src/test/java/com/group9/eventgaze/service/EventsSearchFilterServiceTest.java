package com.group9.eventgaze.service;

import com.group9.eventgaze.entity.Events;
import com.group9.eventgaze.repository.EventsRepo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventsSearchFilterServiceTest {

    @Mock
    private EventsRepo eventsRepo;

    @InjectMocks
    private EventsService eventsService;

    @Test
    void testSearchEvents_WithQuery() {
        String query = "hackathon";
        Pageable pageable = PageRequest.of(0, 10);
        Events event = new Events();
        event.setEventId(1L);
        event.setEventName("Spring Hackathon");

        Page<Events> page = new PageImpl<>(List.of(event), pageable, 1);
        when(eventsRepo.findByEventNameContainingIgnoreCaseOrEventDescriptionContainingIgnoreCaseOrEventTagsContainingIgnoreCase(
                eq("hackathon"), eq("hackathon"), eq("hackathon"), eq(pageable)))
                .thenReturn(page);

        Page<Events> result = eventsService.searchEvents(query, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Spring Hackathon", result.getContent().get(0).getEventName());
        verify(eventsRepo).findByEventNameContainingIgnoreCaseOrEventDescriptionContainingIgnoreCaseOrEventTagsContainingIgnoreCase(
                "hackathon", "hackathon", "hackathon", pageable);
    }

    @Test
    void testSearchEvents_WithNullQueryDefaultsToEmptyString() {
        Pageable pageable = PageRequest.of(0, 5);
        Page<Events> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        when(eventsRepo.findByEventNameContainingIgnoreCaseOrEventDescriptionContainingIgnoreCaseOrEventTagsContainingIgnoreCase(
                eq(""), eq(""), eq(""), eq(pageable)))
                .thenReturn(emptyPage);

        Page<Events> result = eventsService.searchEvents(null, pageable);

        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
        verify(eventsRepo).findByEventNameContainingIgnoreCaseOrEventDescriptionContainingIgnoreCaseOrEventTagsContainingIgnoreCase(
                "", "", "", pageable);
    }

    @Test
    void testGetUpcomingEvents() {
        Pageable pageable = PageRequest.of(0, 10);
        Events event = new Events();
        event.setEventId(2L);
        event.setEventName("Upcoming Career Fair");
        event.setEventDate(LocalDate.now().plusDays(5));

        Page<Events> page = new PageImpl<>(List.of(event), pageable, 1);
        when(eventsRepo.findByEventDateGreaterThanEqual(eq(LocalDate.now()), eq(pageable)))
                .thenReturn(page);

        Page<Events> result = eventsService.getUpcomingEvents(pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Upcoming Career Fair", result.getContent().get(0).getEventName());
        verify(eventsRepo).findByEventDateGreaterThanEqual(eq(LocalDate.now()), eq(pageable));
    }

    @Test
    void testGetPastEvents() {
        Pageable pageable = PageRequest.of(0, 10);
        Events event = new Events();
        event.setEventId(3L);
        event.setEventName("Past Orientation");
        event.setEventDate(LocalDate.now().minusDays(10));

        Page<Events> page = new PageImpl<>(List.of(event), pageable, 1);
        when(eventsRepo.findByEventDateLessThan(eq(LocalDate.now()), eq(pageable)))
                .thenReturn(page);

        Page<Events> result = eventsService.getPastEvents(pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Past Orientation", result.getContent().get(0).getEventName());
        verify(eventsRepo).findByEventDateLessThan(eq(LocalDate.now()), eq(pageable));
    }
}
