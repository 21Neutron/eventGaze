package com.group9.eventgaze.controller;

import com.group9.eventgaze.entity.Events;
import com.group9.eventgaze.entity.dto.EventRequestDTO;
import com.group9.eventgaze.entity.dto.EventResponseDTO;
import com.group9.eventgaze.service.EventCategoryService;
import com.group9.eventgaze.service.EventsService;
import com.group9.eventgaze.utils.EventMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("eventgaze/events")
public class EventsController {

    @Autowired
    private EventsService eventsService;

    @Autowired
    private EventCategoryService eventCategoryService;

    // Returns all events (supports optional pagination: ?page=0&size=10&sortBy=eventDate&direction=DESC)
    @GetMapping("/getAll")
    public ResponseEntity<?> getAllEvents(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(defaultValue = "eventId") String sortBy,
            @RequestParam(defaultValue = "ASC") String direction) {

        if (page != null && size != null) {
            Sort sort = direction.equalsIgnoreCase("DESC") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
            Pageable pageable = PageRequest.of(page, size, sort);
            Page<Events> pagedEvents = eventsService.getAllEvents(pageable);
            Page<EventResponseDTO> responsePage = pagedEvents.map(EventMapper::toEventResponseDTO);
            return ResponseEntity.ok(responsePage);
        }

        List<Events> events = eventsService.getAllEvents();
        if (events.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } else {
            return new ResponseEntity<>(EventMapper.toEventResponseDTOList(events), HttpStatus.OK);
        }
    }

    // Search events by keyword across eventName, eventDescription, and eventTags with pagination
    @GetMapping("/search")
    public ResponseEntity<Page<EventResponseDTO>> searchEvents(
            @RequestParam(required = false, defaultValue = "") String query,
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "eventId") String sortBy,
            @RequestParam(defaultValue = "ASC") String direction) {

        int pageNum = (page != null) ? page : 0;
        int pageSize = (size != null) ? size : 10;
        String sortField = (sortBy != null && !sortBy.isBlank()) ? sortBy : "eventId";
        Sort sort = (direction != null && direction.equalsIgnoreCase("DESC"))
                ? Sort.by(sortField).descending()
                : Sort.by(sortField).ascending();
        Pageable pageable = PageRequest.of(pageNum, pageSize, sort);
        Page<Events> pagedEvents = eventsService.searchEvents(query, pageable);
        return ResponseEntity.ok(pagedEvents.map(EventMapper::toEventResponseDTO));
    }

    // Get upcoming events (eventDate >= today) with pagination
    @GetMapping("/upcoming")
    public ResponseEntity<Page<EventResponseDTO>> getUpcomingEvents(
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "eventId") String sortBy,
            @RequestParam(defaultValue = "ASC") String direction) {

        int pageNum = (page != null) ? page : 0;
        int pageSize = (size != null) ? size : 10;
        String sortField = (sortBy != null && !sortBy.isBlank()) ? sortBy : "eventId";
        Sort sort = (direction != null && direction.equalsIgnoreCase("DESC"))
                ? Sort.by(sortField).descending()
                : Sort.by(sortField).ascending();
        Pageable pageable = PageRequest.of(pageNum, pageSize, sort);
        Page<Events> pagedEvents = eventsService.getUpcomingEvents(pageable);
        return ResponseEntity.ok(pagedEvents.map(EventMapper::toEventResponseDTO));
    }

    // Get past events (eventDate < today) with pagination
    @GetMapping("/past")
    public ResponseEntity<Page<EventResponseDTO>> getPastEvents(
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "eventId") String sortBy,
            @RequestParam(defaultValue = "ASC") String direction) {

        int pageNum = (page != null) ? page : 0;
        int pageSize = (size != null) ? size : 10;
        String sortField = (sortBy != null && !sortBy.isBlank()) ? sortBy : "eventId";
        Sort sort = (direction != null && direction.equalsIgnoreCase("DESC"))
                ? Sort.by(sortField).descending()
                : Sort.by(sortField).ascending();
        Pageable pageable = PageRequest.of(pageNum, pageSize, sort);
        Page<Events> pagedEvents = eventsService.getPastEvents(pageable);
        return ResponseEntity.ok(pagedEvents.map(EventMapper::toEventResponseDTO));
    }

    // For creating a new event via JSON payload
    @PostMapping(value = "/create", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> createEventJson(@RequestBody EventRequestDTO requestDTO) throws IOException {
        eventsService.createEvent(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // For creating a new event via multipart/form-data or form parameters
    @PostMapping("/create")
    public ResponseEntity<String> createEvent(@ModelAttribute EventRequestDTO requestDTO) throws IOException {
        eventsService.createEvent(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // For getting event by ID
    @GetMapping("eventId/id/{myId}")
    public ResponseEntity<EventResponseDTO> getEventById(@PathVariable Long myId) {
        Optional<Events> optionalEvent = eventsService.findEventById(myId);

        if (optionalEvent.isPresent()) {
            Events event = optionalEvent.get();
            return new ResponseEntity<>(EventMapper.toEventResponseDTO(event), HttpStatus.OK);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    // Fetch event with categoryId (supports optional pagination)
    @GetMapping("/category/id/{categoryId}")
    public ResponseEntity<?> getEventsByCategory(
            @PathVariable Long categoryId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(defaultValue = "eventId") String sortBy,
            @RequestParam(defaultValue = "ASC") String direction) {

        if (page != null && size != null) {
            Sort sort = direction.equalsIgnoreCase("DESC") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
            Pageable pageable = PageRequest.of(page, size, sort);
            Page<Events> pagedEvents = eventsService.getEventsByCategory(categoryId, pageable);
            return ResponseEntity.ok(pagedEvents.map(EventMapper::toEventResponseDTO));
        }

        List<Events> events = eventsService.getEventsByCategory(categoryId);
        if (events.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(EventMapper.toEventResponseDTOList(events), HttpStatus.OK);
    }

    // Fetch events with college id (supports optional pagination)
    @GetMapping("college/id/{collegeId}")
    public ResponseEntity<?> getEventsByCollegeId(
            @PathVariable Long collegeId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(defaultValue = "eventId") String sortBy,
            @RequestParam(defaultValue = "ASC") String direction) {

        if (page != null && size != null) {
            Sort sort = direction.equalsIgnoreCase("DESC") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
            Pageable pageable = PageRequest.of(page, size, sort);
            Page<Events> pagedEvents = eventsService.getEventsByCollegeId(collegeId, pageable);
            return ResponseEntity.ok(pagedEvents.map(EventMapper::toEventResponseDTO));
        }

        List<Events> events = eventsService.getEventsByCollegeId(collegeId);
        if (events.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return new ResponseEntity<>(EventMapper.toEventResponseDTOList(events), HttpStatus.OK);
    }

    // For deleting event by ID
    @DeleteMapping("id/{myId}")
    public ResponseEntity<Void> deleteEventById(@PathVariable Long myId) {
        eventsService.deleteEventById(myId);
        return ResponseEntity.noContent().build();
    }

    // For updating or editing an event
    @PutMapping("eventEdit/id/{myId}")
    public ResponseEntity<Events> editByEventId(@PathVariable Long myId, @RequestBody Events newEvent) {
        Events updatedEvent = eventsService.updateEventById(myId, newEvent);
        return new ResponseEntity<>(updatedEvent, HttpStatus.OK);
    }
}
