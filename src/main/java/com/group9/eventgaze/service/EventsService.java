package com.group9.eventgaze.service;

import com.group9.eventgaze.entity.*;
import com.group9.eventgaze.entity.dto.CustomUserDetails;
import com.group9.eventgaze.entity.dto.EventRequestDTO;
import com.group9.eventgaze.repository.CollegesRepo;
import com.group9.eventgaze.repository.EventsRepo;
import com.group9.eventgaze.repository.PublishersRepo;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(EventsService.class);

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

    public List<Events> getEvents() {
        return eventsRepo.findAll();
    }

    public Page<Events> getEvents(Pageable pageable) {
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

        deleteArtworkIfPresent(event.getEventArt());

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
            if (newEvent.getEventArt() != null) {
                if (!newEvent.getEventArt().equals(oldEvent.getEventArt())) {
                    if (oldEvent.getEventArt() != null && !oldEvent.getEventArt().isBlank()) {
                        deleteArtworkIfPresent(oldEvent.getEventArt());
                    }
                    oldEvent.setEventArt(newEvent.getEventArt().isBlank() ? null : newEvent.getEventArt());
                }
            }

            eventsRepo.save(oldEvent);
        }
        return oldEvent;
    }

    /**
     * Extracts the S3 object key from an S3 artwork URL.
     *
     * @param url S3 URL or raw key
     * @return Extracted S3 key, or null
     */
    public String extractKeyFromUrl(String url) {
        return S3Service.extractKeyFromUrl(url, s3Service != null ? s3Service.getBucketName() : null);
    }

    /**
     * Checks if the artwork URL or key represents a default placeholder.
     *
     * @param urlOrKey The URL or key to check
     * @return true if placeholder, false otherwise
     */
    public boolean isDefaultPlaceholder(String urlOrKey) {
        return S3Service.isPlaceholder(urlOrKey);
    }

    /**
     * Deletes the event artwork from S3 if present and not a default placeholder.
     * Gracefully ignores any deletion errors to prevent blocking DB operations.
     *
     * @param artworkUrl The artwork URL to delete
     */
    private void deleteArtworkIfPresent(String artworkUrl) {
        if (artworkUrl == null || artworkUrl.isBlank()) {
            return;
        }
        if (s3Service == null) {
            return;
        }
        if (isDefaultPlaceholder(artworkUrl)) {
            log.info("Skipping S3 deletion for default placeholder artwork: {}", artworkUrl);
            return;
        }
        try {
            String key = extractKeyFromUrl(artworkUrl);
            if (key != null && !key.isBlank()) {
                if (isDefaultPlaceholder(key)) {
                    log.info("Skipping S3 deletion for placeholder key: {}", key);
                    return;
                }
                s3Service.deleteFile(key);
            }
        } catch (Exception e) {
            log.error("Failed to delete S3 artwork for URL '{}': {}", artworkUrl, e.getMessage(), e);
        }
    }

    public Page<Events> searchEvents(String query, Pageable pageable) {
        String searchStr = (query != null) ? query : "";
        return eventsRepo.findByEventNameContainingIgnoreCaseOrEventDescriptionContainingIgnoreCaseOrEventTagsContainingIgnoreCase(
                searchStr, searchStr, searchStr, pageable);
    }

    public Page<Events> getUpcomingEvents(Pageable pageable) {
        return eventsRepo.findByEventDateGreaterThanEqual(LocalDate.now(), pageable);
    }

    public Page<Events> getPastEvents(Pageable pageable) {
        return eventsRepo.findByEventDateLessThan(LocalDate.now(), pageable);
    }
}
