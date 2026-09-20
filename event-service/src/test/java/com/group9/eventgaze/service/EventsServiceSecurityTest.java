package com.group9.eventgaze.service;

import com.group9.eventgaze.entity.*;
import com.group9.eventgaze.entity.dto.CustomUserDetails;
import com.group9.eventgaze.entity.dto.EventRequestDTO;
import com.group9.eventgaze.repository.CollegesRepo;
import com.group9.eventgaze.repository.EventsRepo;
import com.group9.eventgaze.repository.PublishersRepo;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventsServiceSecurityTest {

    @Mock
    private EventsRepo eventsRepo;

    @Mock
    private EventCategoryService eventCategoryService;

    @Mock
    private S3Service s3Service;

    @Mock
    private CollegesRepo collegesRepo;

    @Mock
    private CollegesService collegesService;

    @Mock
    private PublishersRepo publishersRepo;

    @Mock
    private PublishersService publishersService;

    @Mock
    private UsersService usersService;

    @InjectMocks
    private EventsService eventsService;

    private CustomUserDetails publisherDetails;

    @BeforeEach
    void setUp() {
        publisherDetails = new CustomUserDetails(10L, "PUBLISHER");
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(publisherDetails, null, publisherDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testCreateEventUsesAuthenticatedUserNotDtoUserId() throws IOException {
        EventRequestDTO requestDTO = new EventRequestDTO();
        requestDTO.setEventName("Design Thinking");
        requestDTO.setEventDescription("Workshop");
        requestDTO.setEventDate("20-11-2026");
        requestDTO.setEventCategoryId(1L);
        requestDTO.setColleges(2L);
        requestDTO.setUserId(999L); // Attacker tries to spoof userId 999!

        EventCategory category = new EventCategory();
        category.setCategoryId(1L);
        when(eventCategoryService.findByIdOrThrow(1L)).thenReturn(category);

        Users authenticatedUser = new Users();
        authenticatedUser.setUserId(10L); // Authenticated user ID is 10
        when(usersService.findByIdOrThrow(10L)).thenReturn(authenticatedUser);

        Publishers publisher = new Publishers();
        publisher.setPublisherId(5L);
        publisher.setUsers(authenticatedUser);
        when(publishersService.findByUser(authenticatedUser)).thenReturn(publisher);

        Colleges college = new Colleges();
        college.setCollegeId(2L);
        when(collegesService.findById(2L)).thenReturn(college);

        when(eventsRepo.save(any(Events.class))).thenAnswer(invocation -> invocation.getArgument(0));

        eventsService.createEvent(requestDTO);

        // Verify usersService was called with authenticated user (10L), NOT spoofed user (999L)!
        verify(usersService).findByIdOrThrow(10L);
        verify(usersService, never()).findByIdOrThrow(999L);
        verify(publishersService).findByUser(authenticatedUser);
        verify(eventsRepo).save(any(Events.class));
    }

    @Test
    void testCreateEventWithExistingArtworkUrl() throws IOException {
        EventRequestDTO requestDTO = new EventRequestDTO();
        requestDTO.setEventName("Cloud Summit 2026");
        requestDTO.setEventDescription("Cloud Architecture");
        requestDTO.setEventDate("25-11-2026");
        requestDTO.setEventCategoryId(1L);
        requestDTO.setColleges(2L);
        requestDTO.setEventArtUrl("https://bucket.s3.amazonaws.com/events/existing-image.jpg");

        EventCategory category = new EventCategory();
        category.setCategoryId(1L);
        when(eventCategoryService.findByIdOrThrow(1L)).thenReturn(category);

        Users authenticatedUser = new Users();
        authenticatedUser.setUserId(10L);
        when(usersService.findByIdOrThrow(10L)).thenReturn(authenticatedUser);

        Publishers publisher = new Publishers();
        publisher.setPublisherId(5L);
        publisher.setUsers(authenticatedUser);
        when(publishersService.findByUser(authenticatedUser)).thenReturn(publisher);

        Colleges college = new Colleges();
        college.setCollegeId(2L);
        when(collegesService.findById(2L)).thenReturn(college);

        when(eventsRepo.save(any(Events.class))).thenAnswer(invocation -> invocation.getArgument(0));

        eventsService.createEvent(requestDTO);

        verify(s3Service, never()).uploadFile(any(), any());
        verify(eventsRepo, times(2)).save(argThat(event ->
                "https://bucket.s3.amazonaws.com/events/existing-image.jpg".equals(event.getEventArt())
        ));
    }

    @Test
    void testDeleteEventByIdOwnerSuccess() {
        Users ownerUser = new Users();
        ownerUser.setUserId(10L);

        Publishers publisher = new Publishers();
        publisher.setUsers(ownerUser);

        Events event = new Events();
        event.setEventId(1L);
        event.setPublishers(publisher);

        when(eventsRepo.findById(1L)).thenReturn(Optional.of(event));

        eventsService.deleteEventById(1L);

        verify(eventsRepo).delete(event);
    }

    @Test
    void testDeleteEventByIdNotOwnerThrowsAccessDenied() {
        Users otherUser = new Users();
        otherUser.setUserId(99L); // Different owner!

        Publishers publisher = new Publishers();
        publisher.setUsers(otherUser);

        Events event = new Events();
        event.setEventId(1L);
        event.setPublishers(publisher);

        when(eventsRepo.findById(1L)).thenReturn(Optional.of(event));

        assertThrows(AccessDeniedException.class, () -> eventsService.deleteEventById(1L));
        verify(eventsRepo, never()).delete(any());
    }

    @Test
    void testUpdateEventByIdOwnerSuccess() {
        Users ownerUser = new Users();
        ownerUser.setUserId(10L);

        Publishers publisher = new Publishers();
        publisher.setUsers(ownerUser);

        Events event = new Events();
        event.setEventId(1L);
        event.setEventName("Old Name");
        event.setPublishers(publisher);

        when(eventsRepo.findById(1L)).thenReturn(Optional.of(event));
        when(eventsRepo.save(any(Events.class))).thenAnswer(inv -> inv.getArgument(0));

        Events updateData = new Events();
        updateData.setEventName("New Name");

        Events result = eventsService.updateEventById(1L, updateData);

        assertEquals("New Name", result.getEventName());
        verify(eventsRepo).save(event);
    }

    @Test
    void testUpdateEventByIdNotOwnerThrowsAccessDenied() {
        Users otherUser = new Users();
        otherUser.setUserId(99L); // Different user

        Publishers publisher = new Publishers();
        publisher.setUsers(otherUser);

        Events event = new Events();
        event.setEventId(1L);
        event.setPublishers(publisher);

        when(eventsRepo.findById(1L)).thenReturn(Optional.of(event));

        Events updateData = new Events();
        updateData.setEventName("New Name");

        assertThrows(AccessDeniedException.class, () -> eventsService.updateEventById(1L, updateData));
        verify(eventsRepo, never()).save(any());
    }

    @Test
    void testUpdateEventByIdNotFoundThrowsEntityNotFound() {
        when(eventsRepo.findById(999L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> eventsService.updateEventById(999L, new Events()));
    }

    @Test
    void testDeleteEventByIdWithArtworkTriggersS3Deletion() {
        Users ownerUser = new Users();
        ownerUser.setUserId(10L);

        Publishers publisher = new Publishers();
        publisher.setUsers(ownerUser);

        Events event = new Events();
        event.setEventId(1L);
        event.setPublishers(publisher);
        event.setEventArt("https://eventgazeeventimages.s3.amazonaws.com/events/flyer-123.png");

        when(eventsRepo.findById(1L)).thenReturn(Optional.of(event));

        eventsService.deleteEventById(1L);

        verify(s3Service).deleteFile("events/flyer-123.png");
        verify(eventsRepo).delete(event);
    }

    @Test
    void testDeleteEventByIdWithRegionalArtworkUrlTriggersS3Deletion() {
        Users ownerUser = new Users();
        ownerUser.setUserId(10L);

        Publishers publisher = new Publishers();
        publisher.setUsers(ownerUser);

        Events event = new Events();
        event.setEventId(2L);
        event.setPublishers(publisher);
        event.setEventArt("https://bucket.s3.region.amazonaws.com/events/key.png");

        when(eventsRepo.findById(2L)).thenReturn(Optional.of(event));

        eventsService.deleteEventById(2L);

        verify(s3Service).deleteFile("events/key.png");
        verify(eventsRepo).delete(event);
    }

    @Test
    void testDeleteEventByIdWithPlaceholderArtworkDoesNotTriggerS3Deletion() {
        Users ownerUser = new Users();
        ownerUser.setUserId(10L);

        Publishers publisher = new Publishers();
        publisher.setUsers(ownerUser);

        Events event = new Events();
        event.setEventId(3L);
        event.setPublishers(publisher);
        event.setEventArt("https://bucket.s3.amazonaws.com/events/default-poster.png");

        when(eventsRepo.findById(3L)).thenReturn(Optional.of(event));

        eventsService.deleteEventById(3L);

        verify(s3Service, never()).deleteFile(anyString());
        verify(eventsRepo).delete(event);
    }

    @Test
    void testDeleteEventByIdWhenS3DeletionFailsStillDeletesFromRepo() {
        Users ownerUser = new Users();
        ownerUser.setUserId(10L);

        Publishers publisher = new Publishers();
        publisher.setUsers(ownerUser);

        Events event = new Events();
        event.setEventId(4L);
        event.setPublishers(publisher);
        event.setEventArt("https://bucket.s3.amazonaws.com/events/fail-flyer.png");

        when(eventsRepo.findById(4L)).thenReturn(Optional.of(event));
        doThrow(new RuntimeException("Simulated S3 failure")).when(s3Service).deleteFile(anyString());

        assertDoesNotThrow(() -> eventsService.deleteEventById(4L));
        verify(eventsRepo).delete(event);
    }

    @Test
    void testUpdateEventByIdWithNewArtworkTriggersOldArtworkDeletion() {
        Users ownerUser = new Users();
        ownerUser.setUserId(10L);

        Publishers publisher = new Publishers();
        publisher.setUsers(ownerUser);

        Events oldEvent = new Events();
        oldEvent.setEventId(5L);
        oldEvent.setPublishers(publisher);
        oldEvent.setEventArt("https://bucket.s3.amazonaws.com/events/old-art.png");

        when(eventsRepo.findById(5L)).thenReturn(Optional.of(oldEvent));
        when(eventsRepo.save(any(Events.class))).thenAnswer(inv -> inv.getArgument(0));

        Events updateData = new Events();
        updateData.setEventArt("https://bucket.s3.amazonaws.com/events/new-art.png");

        Events result = eventsService.updateEventById(5L, updateData);

        verify(s3Service).deleteFile("events/old-art.png");
        assertEquals("https://bucket.s3.amazonaws.com/events/new-art.png", result.getEventArt());
        verify(eventsRepo).save(oldEvent);
    }

    @Test
    void testUpdateEventByIdSameArtworkDoesNotTriggerS3Deletion() {
        Users ownerUser = new Users();
        ownerUser.setUserId(10L);

        Publishers publisher = new Publishers();
        publisher.setUsers(ownerUser);

        Events oldEvent = new Events();
        oldEvent.setEventId(6L);
        oldEvent.setPublishers(publisher);
        oldEvent.setEventArt("https://bucket.s3.amazonaws.com/events/same-art.png");

        when(eventsRepo.findById(6L)).thenReturn(Optional.of(oldEvent));
        when(eventsRepo.save(any(Events.class))).thenAnswer(inv -> inv.getArgument(0));

        Events updateData = new Events();
        updateData.setEventArt("https://bucket.s3.amazonaws.com/events/same-art.png");

        eventsService.updateEventById(6L, updateData);

        verify(s3Service, never()).deleteFile(anyString());
        verify(eventsRepo).save(oldEvent);
    }

    @Test
    void testUpdateEventByIdNullArtworkPreservesOldArtworkAndDoesNotTriggerS3Deletion() {
        Users ownerUser = new Users();
        ownerUser.setUserId(10L);

        Publishers publisher = new Publishers();
        publisher.setUsers(ownerUser);

        Events oldEvent = new Events();
        oldEvent.setEventId(7L);
        oldEvent.setPublishers(publisher);
        oldEvent.setEventArt("https://bucket.s3.amazonaws.com/events/stay-art.png");

        when(eventsRepo.findById(7L)).thenReturn(Optional.of(oldEvent));
        when(eventsRepo.save(any(Events.class))).thenAnswer(inv -> inv.getArgument(0));

        Events updateData = new Events();
        updateData.setEventName("Updated Title Only");

        Events result = eventsService.updateEventById(7L, updateData);

        verify(s3Service, never()).deleteFile(anyString());
        assertEquals("https://bucket.s3.amazonaws.com/events/stay-art.png", result.getEventArt());
        verify(eventsRepo).save(oldEvent);
    }
}
