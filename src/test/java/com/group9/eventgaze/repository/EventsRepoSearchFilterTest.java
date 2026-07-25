package com.group9.eventgaze.repository;

import com.group9.eventgaze.entity.Events;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
class EventsRepoSearchFilterTest {

    @Autowired
    private EventsRepo eventsRepo;

    @BeforeEach
    void setUp() {
        eventsRepo.deleteAll();

        Events event1 = new Events();
        event1.setEventName("Spring Boot Workshop");
        event1.setEventDescription("Learn backend engineering with Java and Spring");
        event1.setEventTags("java,spring,backend");
        event1.setEventDate(LocalDate.now().plusDays(10));
        eventsRepo.save(event1);

        Events event2 = new Events();
        event2.setEventName("Design Symposium");
        event2.setEventDescription("UI and UX discussions");
        event2.setEventTags("design,figma,frontend");
        event2.setEventDate(LocalDate.now().minusDays(5));
        eventsRepo.save(event2);

        Events event3 = new Events();
        event3.setEventName("AI & Machine Learning Seminar");
        event3.setEventDescription("Deep dive into modern LLMs");
        event3.setEventTags("ai,ml,python");
        event3.setEventDate(LocalDate.now()); // today
        eventsRepo.save(event3);
    }

    @Test
    void testKeywordSearch_ByEventName() {
        Page<Events> results = eventsRepo.findByEventNameContainingIgnoreCaseOrEventDescriptionContainingIgnoreCaseOrEventTagsContainingIgnoreCase(
                "workshop", "workshop", "workshop", PageRequest.of(0, 10));

        assertEquals(1, results.getTotalElements());
        assertEquals("Spring Boot Workshop", results.getContent().get(0).getEventName());
    }

    @Test
    void testKeywordSearch_ByEventDescription() {
        Page<Events> results = eventsRepo.findByEventNameContainingIgnoreCaseOrEventDescriptionContainingIgnoreCaseOrEventTagsContainingIgnoreCase(
                "discussions", "discussions", "discussions", PageRequest.of(0, 10));

        assertEquals(1, results.getTotalElements());
        assertEquals("Design Symposium", results.getContent().get(0).getEventName());
    }

    @Test
    void testKeywordSearch_ByEventTags() {
        Page<Events> results = eventsRepo.findByEventNameContainingIgnoreCaseOrEventDescriptionContainingIgnoreCaseOrEventTagsContainingIgnoreCase(
                "python", "python", "python", PageRequest.of(0, 10));

        assertEquals(1, results.getTotalElements());
        assertEquals("AI & Machine Learning Seminar", results.getContent().get(0).getEventName());
    }

    @Test
    void testKeywordSearch_CaseInsensitive() {
        Page<Events> results = eventsRepo.findByEventNameContainingIgnoreCaseOrEventDescriptionContainingIgnoreCaseOrEventTagsContainingIgnoreCase(
                "SPRING", "SPRING", "SPRING", PageRequest.of(0, 10));

        assertEquals(1, results.getTotalElements());
        assertEquals("Spring Boot Workshop", results.getContent().get(0).getEventName());
    }

    @Test
    void testUpcomingEvents_ReturnsTodayAndFutureEvents() {
        Page<Events> results = eventsRepo.findByEventDateGreaterThanEqual(
                LocalDate.now(), PageRequest.of(0, 10, Sort.by("eventDate").ascending()));

        // Both event1 (in 10 days) and event3 (today) are upcoming
        assertEquals(2, results.getTotalElements());
        assertTrue(results.getContent().stream().noneMatch(e -> e.getEventName().equals("Design Symposium")));
    }

    @Test
    void testPastEvents_ReturnsOnlyPastEvents() {
        Page<Events> results = eventsRepo.findByEventDateLessThan(
                LocalDate.now(), PageRequest.of(0, 10, Sort.by("eventDate").descending()));

        // Only event2 (5 days ago) is past
        assertEquals(1, results.getTotalElements());
        assertEquals("Design Symposium", results.getContent().get(0).getEventName());
    }
}
