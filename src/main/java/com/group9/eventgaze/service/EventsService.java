package com.group9.eventgaze.service;

import com.group9.eventgaze.entity.*;
import com.group9.eventgaze.entity.dto.CustomUserDetails;
import com.group9.eventgaze.entity.dto.EventRequestDTO;
import com.group9.eventgaze.repository.CollegesRepo;
import com.group9.eventgaze.repository.EventsRepo;
import com.group9.eventgaze.repository.PublishersRepo;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
public class EventsService {

    @Autowired
    private EventsRepo eventsRepo;

    @Autowired
    private EventCategoryService eventCategoryService;

    @Autowired
    private S3Service s3Service;

    @Autowired
    private CollegesRepo collegesRepo;

    @Autowired
    private CollegesService collegesService;

    @Autowired
    private PublishersRepo publishersRepo;

    @Autowired
    private PublishersService publishersService;

    @Autowired
    private UsersService usersService;

    public List<Events> getAllEvents() {
        return eventsRepo.findAll();
    }

    public Page<Events> getAllEvents(Pageable pageable) {
        return eventsRepo.findAll(pageable);
    }

    public Long getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            throw new AccessDeniedException("User is not authenticated");
        }
        return userDetails.getUserId();
    }

    public void verifyEventOwnership(Events event, Long authenticatedUserId) {
        if (event == null || event.getPublishers() == null || event.getPublishers().getUsers() == null ||
                !event.getPublishers().getUsers().getUserId().equals(authenticatedUserId)) {
            throw new AccessDeniedException("You do not have permission to modify this event");
        }
    }

    // for creating an event
    public void createEvent(EventRequestDTO eventRequestDTO) throws IOException {
        Long authenticatedUserId = getAuthenticatedUserId();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        LocalDate eventDate = LocalDate.parse(eventRequestDTO.getEventDate(), formatter);

        EventCategory eventCategory = eventCategoryService.findByIdOrThrow(eventRequestDTO.getEventCategoryId());

        Users user = usersService.findByIdOrThrow(authenticatedUserId);
        Publishers publishers = publishersService.findByUser(user);

        Events event = new Events();
        event.setEventName(eventRequestDTO.getEventName());
        event.setEventDescription(eventRequestDTO.getEventDescription());
        event.setEventDate(eventDate);
        event.setEventTags(eventRequestDTO.getEventTags());
        event.setEventCategory(eventCategory);
        event.setPublishers(publishers);

        Colleges college = collegesService.findById(eventRequestDTO.getColleges()); // Find the college by its ID
        event.setCollege(college);

        Events savedEvent = eventsRepo.save(event);

        if (eventRequestDTO.getEventArt() != null && !eventRequestDTO.getEventArt().isEmpty()) {
            String fileUrl = s3Service.uploadFile("events", eventRequestDTO.getEventArt());
            savedEvent.setEventArt(fileUrl);

            eventsRepo.save(savedEvent);
        }
    }

    // For finding events by event ID
    public Optional<Events> findEventById(Long myId) {
        return eventsRepo.findById(myId);
    }

    // For fetching event by the event category ID
    public List<Events> getEventsByCategory(Long categoryId) {
        return eventsRepo.findByEventCategory_CategoryId(categoryId);
    }

    public Page<Events> getEventsByCategory(Long categoryId, Pageable pageable) {
        return eventsRepo.findByEventCategory_CategoryId(categoryId, pageable);
    }

    // For fetching events by college ID
    public List<Events> getEventsByCollegeId(Long collegeId) {
        return eventsRepo.findByCollege_CollegeId(collegeId);
    }

    public Page<Events> getEventsByCollegeId(Long collegeId, Pageable pageable) {
        return eventsRepo.findByCollege_CollegeId(collegeId, pageable);
    }

    // For deleting an event with the eventId
    public void deleteEventById(Long myId) {
        Events event = eventsRepo.findById(myId)
                .orElseThrow(() -> new EntityNotFoundException("Event with ID " + myId + " not found."));

        Long authenticatedUserId = getAuthenticatedUserId();
        verifyEventOwnership(event, authenticatedUserId);

        eventsRepo.delete(event);
    }

    public Events updateEventById(Long myId, Events newEvent) {
        Events oldEvent = eventsRepo.findById(myId)
                .orElseThrow(() -> new EntityNotFoundException("Event with ID " + myId + " not found."));

        Long authenticatedUserId = getAuthenticatedUserId();
        verifyEventOwnership(oldEvent, authenticatedUserId);

        if (newEvent != null) {
            oldEvent.setEventName(newEvent.getEventName() != null && !newEvent.getEventName().isEmpty() ? newEvent.getEventName() : oldEvent.getEventName());
            oldEvent.setEventDescription(newEvent.getEventDescription() != null && !newEvent.getEventDescription().isEmpty() ? newEvent.getEventDescription() : oldEvent.getEventDescription());
            oldEvent.setEventTags(newEvent.getEventTags() != null && !newEvent.getEventTags().isEmpty() ? newEvent.getEventTags() : oldEvent.getEventTags());

            if (newEvent.getEventDate() != null) {
                oldEvent.setEventDate(newEvent.getEventDate());
            }
            if (newEvent.getEventVenue() != null && !newEvent.getEventVenue().isEmpty()) {
                oldEvent.setEventVenue(newEvent.getEventVenue());
            }
            if (newEvent.getCollege() != null) {
                oldEvent.setCollege(newEvent.getCollege());
            }
            if (newEvent.getEventCategory() != null) {
                oldEvent.setEventCategory(newEvent.getEventCategory());
            }

            eventsRepo.save(oldEvent);
        }
        return oldEvent;
    }
}
