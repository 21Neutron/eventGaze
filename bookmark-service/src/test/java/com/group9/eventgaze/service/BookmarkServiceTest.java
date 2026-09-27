package com.group9.eventgaze.service;

import com.group9.eventgaze.entity.Events;
import com.group9.eventgaze.entity.StudentBookmarks;
import com.group9.eventgaze.entity.Students;
import com.group9.eventgaze.entity.Users;
import com.group9.eventgaze.entity.dto.EventResponseDTO;
import com.group9.eventgaze.repository.EventsRepo;
import com.group9.eventgaze.repository.StudentBookmarksRepo;
import com.group9.eventgaze.repository.StudentsRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookmarkServiceTest {

    @Mock
    private StudentBookmarksRepo studentBookmarksRepo;

    @Mock
    private StudentsRepo studentsRepo;

    @Mock
    private EventsRepo eventsRepo;

    @InjectMocks
    private BookmarkService bookmarkService;

    private Students mockStudent;
    private Events mockEvent;

    @BeforeEach
    void setUp() {
        Users mockUser = new Users();
        mockUser.setUserId(10L);
        mockUser.setUserEmail("student@test.com");
        mockUser.setUserRole("STUDENT");

        mockStudent = new Students();
        mockStudent.setStudentId(100L);
        mockStudent.setFirstName("John");
        mockStudent.setLastName("Doe");
        mockStudent.setUsers(mockUser);

        mockEvent = new Events();
        mockEvent.setEventId(200L);
        mockEvent.setEventName("Hackathon 2026");
        mockEvent.setEventDate(LocalDate.of(2026, 12, 1));
        mockEvent.setEventVenue("Main Auditorium");
    }

    @Test
    void testAddBookmarkSuccess() {
        when(studentsRepo.findByUsers_UserId(10L)).thenReturn(Optional.of(mockStudent));
        when(eventsRepo.findById(200L)).thenReturn(Optional.of(mockEvent));
        when(studentBookmarksRepo.existsByStudentEntity_StudentIdAndEvents_EventId(100L, 200L)).thenReturn(false);

        bookmarkService.addBookmark(10L, 200L);

        ArgumentCaptor<StudentBookmarks> captor = ArgumentCaptor.forClass(StudentBookmarks.class);
        verify(studentBookmarksRepo).save(captor.capture());

        StudentBookmarks saved = captor.getValue();
        assertEquals(mockStudent, saved.getStudentEntity());
        assertEquals(mockEvent, saved.getEvents());
        assertNotNull(saved.getCreatedAt());
    }

    @Test
    void testAddBookmarkDuplicateAvoided() {
        when(studentsRepo.findByUsers_UserId(10L)).thenReturn(Optional.of(mockStudent));
        when(eventsRepo.findById(200L)).thenReturn(Optional.of(mockEvent));
        when(studentBookmarksRepo.existsByStudentEntity_StudentIdAndEvents_EventId(100L, 200L)).thenReturn(true);

        bookmarkService.addBookmark(10L, 200L);

        verify(studentBookmarksRepo, never()).save(any());
    }

    @Test
    void testAddBookmarkStudentNotFound() {
        when(studentsRepo.findByUsers_UserId(999L)).thenReturn(Optional.empty());
        when(studentsRepo.findById(999L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                bookmarkService.addBookmark(999L, 200L)
        );
        assertTrue(ex.getMessage().contains("Student not found"));
    }

    @Test
    void testAddBookmarkEventNotFound() {
        when(studentsRepo.findByUsers_UserId(10L)).thenReturn(Optional.of(mockStudent));
        when(eventsRepo.findById(999L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                bookmarkService.addBookmark(10L, 999L)
        );
        assertTrue(ex.getMessage().contains("Event not found"));
    }

    @Test
    void testRemoveBookmarkSuccess() {
        when(studentsRepo.findByUsers_UserId(10L)).thenReturn(Optional.of(mockStudent));
        StudentBookmarks bookmark = new StudentBookmarks();
        bookmark.setBookMarkId(1L);
        bookmark.setStudentEntity(mockStudent);
        bookmark.setEvents(mockEvent);

        when(studentBookmarksRepo.findByStudentEntity_StudentIdAndEvents_EventId(100L, 200L))
                .thenReturn(Optional.of(bookmark));

        bookmarkService.removeBookmark(10L, 200L);

        verify(studentBookmarksRepo).delete(bookmark);
    }

    @Test
    void testRemoveBookmarkWhenNotFound() {
        when(studentsRepo.findByUsers_UserId(10L)).thenReturn(Optional.of(mockStudent));
        when(studentBookmarksRepo.findByStudentEntity_StudentIdAndEvents_EventId(100L, 200L))
                .thenReturn(Optional.empty());

        bookmarkService.removeBookmark(10L, 200L);

        verify(studentBookmarksRepo, never()).delete(any());
    }

    @Test
    void testGetStudentBookmarks() {
        when(studentsRepo.findByUsers_UserId(10L)).thenReturn(Optional.of(mockStudent));
        mockEvent.setEventArt("events/hackathon-poster.jpg");
        mockEvent.setEventArtThumb("events/hackathon-poster-thumb.jpg");
        StudentBookmarks bookmark = new StudentBookmarks();
        bookmark.setBookMarkId(1L);
        bookmark.setStudentEntity(mockStudent);
        bookmark.setEvents(mockEvent);

        when(studentBookmarksRepo.findByStudentEntity_StudentId(100L))
                .thenReturn(List.of(bookmark));

        List<EventResponseDTO> results = bookmarkService.getStudentBookmarks(10L);

        assertEquals(1, results.size());
        assertEquals(200L, results.get(0).getEventId());
        assertEquals("Hackathon 2026", results.get(0).getEventName());
        assertEquals("https://eventgazeeventimages.s3.amazonaws.com/events/hackathon-poster.jpg", results.get(0).getEventArt());
        assertEquals("https://eventgazeeventimages.s3.amazonaws.com/events/hackathon-poster-thumb.jpg", results.get(0).getEventArtThumb());
    }

    @Test
    void testGetStudentBookmarksEmpty() {
        when(studentsRepo.findByUsers_UserId(10L)).thenReturn(Optional.of(mockStudent));
        when(studentBookmarksRepo.findByStudentEntity_StudentId(100L))
                .thenReturn(Collections.emptyList());

        List<EventResponseDTO> results = bookmarkService.getStudentBookmarks(10L);

        assertTrue(results.isEmpty());
    }

    @Test
    void testIsEventBookmarkedTrue() {
        when(studentsRepo.findByUsers_UserId(10L)).thenReturn(Optional.of(mockStudent));
        when(studentBookmarksRepo.existsByStudentEntity_StudentIdAndEvents_EventId(100L, 200L))
                .thenReturn(true);

        boolean result = bookmarkService.isEventBookmarked(10L, 200L);

        assertTrue(result);
    }

    @Test
    void testIsEventBookmarkedFalse() {
        when(studentsRepo.findByUsers_UserId(10L)).thenReturn(Optional.of(mockStudent));
        when(studentBookmarksRepo.existsByStudentEntity_StudentIdAndEvents_EventId(100L, 200L))
                .thenReturn(false);

        boolean result = bookmarkService.isEventBookmarked(10L, 200L);

        assertFalse(result);
    }

    @Test
    void testIsEventBookmarkedStudentNotFound() {
        when(studentsRepo.findByUsers_UserId(999L)).thenReturn(Optional.empty());
        when(studentsRepo.findById(999L)).thenReturn(Optional.empty());

        boolean result = bookmarkService.isEventBookmarked(999L, 200L);

        assertFalse(result);
    }
}
