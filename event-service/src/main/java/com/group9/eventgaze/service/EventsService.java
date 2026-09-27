package com.group9.eventgaze.service;

import com.group9.eventgaze.entity.*;
import com.group9.eventgaze.entity.dto.CustomUserDetails;
import com.group9.eventgaze.entity.dto.EventRequestDTO;
import com.group9.eventgaze.repository.CollegesRepo;
import com.group9.eventgaze.repository.EventsRepo;
import com.group9.eventgaze.repository.PublishersRepo;
import com.group9.eventgaze.utils.StorageUrlResolver;
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

        String bucket = s3Service != null ? s3Service.getBucketName() : null;
        String savedArtKey = null;
        boolean needsUpdate = false;

        if (eventRequestDTO.getEventArt() != null && !eventRequestDTO.getEventArt().isEmpty()) {
            String fileUrl = s3Service.uploadFile("events", eventRequestDTO.getEventArt());
            String relativeKey = StorageUrlResolver.toRelativeKey(fileUrl, bucket);
            savedArtKey = (relativeKey != null ? relativeKey : fileUrl);
            savedEvent.setEventArt(savedArtKey);
            needsUpdate = true;
        } else {
            String artInput = eventRequestDTO.getEventArtKey();
            if (artInput == null || artInput.isBlank()) {
                artInput = eventRequestDTO.getEventArtUrl();
            }
            if (artInput != null && !artInput.isBlank()) {
                String relativeKey = StorageUrlResolver.toRelativeKey(artInput, bucket);
                savedArtKey = (relativeKey != null ? relativeKey : artInput.trim());
                savedEvent.setEventArt(savedArtKey);
                needsUpdate = true;
            }
        }

        String thumbInput = eventRequestDTO.resolveEventArtThumbInput();
        if (thumbInput != null && !thumbInput.isBlank()) {
            String relativeThumbKey = StorageUrlResolver.toRelativeKey(thumbInput, bucket);
            savedEvent.setEventArtThumb(relativeThumbKey != null ? relativeThumbKey : thumbInput.trim());
            needsUpdate = true;
        } else if (savedArtKey != null) {
            String defaultThumbKey = computeDefaultThumbnailKey(savedArtKey);
            if (defaultThumbKey != null) {
                savedEvent.setEventArtThumb(defaultThumbKey);
                needsUpdate = true;
            }
        }

        if (needsUpdate) {
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
        deleteArtworkIfPresent(event.getEventArtThumb());

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
                String bucket = s3Service != null ? s3Service.getBucketName() : null;
                String newArtKey = StorageUrlResolver.toRelativeKey(newEvent.getEventArt(), bucket);
                if (newArtKey == null && !newEvent.getEventArt().isBlank()) {
                    newArtKey = newEvent.getEventArt().trim();
                }
                String oldArtKey = oldEvent.getEventArt();
                boolean isDifferent = (newArtKey == null && oldArtKey != null)
                        || (newArtKey != null && !newArtKey.equals(oldArtKey));

                if (isDifferent) {
                    if (oldArtKey != null && !oldArtKey.isBlank()) {
                        deleteArtworkIfPresent(oldArtKey);
                    }
                    oldEvent.setEventArt(newArtKey);
                }
            }
            if (newEvent.getEventArtThumb() != null) {
                String bucket = s3Service != null ? s3Service.getBucketName() : null;
                String newThumbKey = StorageUrlResolver.toRelativeKey(newEvent.getEventArtThumb(), bucket);
                if (newThumbKey == null && !newEvent.getEventArtThumb().isBlank()) {
                    newThumbKey = newEvent.getEventArtThumb().trim();
                }
                String oldThumbKey = oldEvent.getEventArtThumb();
                boolean isDifferent = (newThumbKey == null && oldThumbKey != null)
                        || (newThumbKey != null && !newThumbKey.equals(oldThumbKey));

                if (isDifferent) {
                    if (oldThumbKey != null && !oldThumbKey.isBlank()) {
                        deleteArtworkIfPresent(oldThumbKey);
                    }
                    oldEvent.setEventArtThumb(newThumbKey);
                }
            } else if (newEvent.getEventArt() != null) {
                String computedThumb = computeDefaultThumbnailKey(oldEvent.getEventArt());
                String oldThumbKey = oldEvent.getEventArtThumb();
                boolean isDifferent = (computedThumb == null && oldThumbKey != null)
                        || (computedThumb != null && !computedThumb.equals(oldThumbKey));
                if (isDifferent) {
                    if (oldThumbKey != null && !oldThumbKey.isBlank()) {
                        deleteArtworkIfPresent(oldThumbKey);
                    }
                    oldEvent.setEventArtThumb(computedThumb);
                }
            }

            eventsRepo.save(oldEvent);
        }
        return oldEvent;
    }

    /**
     * Automatically computes the default thumbnail key <folder>/<uuid>-thumb.jpg
     * if the artwork follows <folder>/<uuid>.jpg.
     */
    public static String computeDefaultThumbnailKey(String artKey) {
        if (artKey == null || artKey.isBlank() || S3Service.isPlaceholder(artKey)) {
            return null;
        }
        String clean = artKey.trim();
        while (clean.startsWith("/")) {
            clean = clean.substring(1);
        }
        if (!clean.contains("/")) {
            return null;
        }
        if (clean.endsWith("-thumb.jpg")) {
            return clean;
        }
        if (clean.endsWith(".jpg")) {
            return clean.substring(0, clean.length() - 4) + "-thumb.jpg";
        }
        int lastDot = clean.lastIndexOf('.');
        if (lastDot != -1) {
            return clean.substring(0, lastDot) + "-thumb" + clean.substring(lastDot);
        }
        return null;
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
     * Accepts relative keys directly or full URLs, extracting the relative S3 key.
     * Gracefully ignores any deletion errors to prevent blocking DB operations.
     *
     * @param artworkKeyOrUrl The artwork key or URL to delete
     */
    public void deleteArtworkIfPresent(String artworkKeyOrUrl) {
        if (artworkKeyOrUrl == null || artworkKeyOrUrl.isBlank()) {
            return;
        }
        if (s3Service == null) {
            return;
        }
        if (isDefaultPlaceholder(artworkKeyOrUrl)) {
            log.info("Skipping S3 deletion for default placeholder artwork: {}", artworkKeyOrUrl);
            return;
        }
        try {
            String bucket = s3Service.getBucketName();
            String key = StorageUrlResolver.toRelativeKey(artworkKeyOrUrl, bucket);
            if (key != null && !key.isBlank()) {
                if (isDefaultPlaceholder(key)) {
                    log.info("Skipping S3 deletion for placeholder key: {}", key);
                    return;
                }
                s3Service.deleteFile(key);
            }
        } catch (Exception e) {
            log.error("Failed to delete S3 artwork for key/URL '{}': {}", artworkKeyOrUrl, e.getMessage(), e);
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
