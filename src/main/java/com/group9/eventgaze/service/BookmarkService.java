package com.group9.eventgaze.service;

import com.group9.eventgaze.entity.Events;
import com.group9.eventgaze.entity.StudentBookmarks;
import com.group9.eventgaze.entity.Students;
import com.group9.eventgaze.entity.dto.EventResponseDTO;
import com.group9.eventgaze.repository.EventsRepo;
import com.group9.eventgaze.repository.StudentBookmarksRepo;
import com.group9.eventgaze.repository.StudentsRepo;
import com.group9.eventgaze.utils.EventMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class BookmarkService {

    @Autowired
    private StudentBookmarksRepo studentBookmarksRepo;

    @Autowired
    private StudentsRepo studentsRepo;

    @Autowired
    private EventsRepo eventsRepo;

    private Students findStudent(Long userId) {
        return studentsRepo.findByUsers_UserId(userId)
                .or(() -> studentsRepo.findById(userId))
                .orElseThrow(() -> new RuntimeException("Student not found for id: " + userId));
    }

    @Transactional
    public void addBookmark(Long userId, Long eventId) {
        Students student = findStudent(userId);
        Events event = eventsRepo.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found with id: " + eventId));

        if (studentBookmarksRepo.existsByStudentEntity_StudentIdAndEvents_EventId(student.getStudentId(), event.getEventId())) {
            return;
        }

        StudentBookmarks bookmark = new StudentBookmarks();
        bookmark.setStudentEntity(student);
        bookmark.setEvents(event);
        bookmark.setCreatedAt(LocalDateTime.now());

        studentBookmarksRepo.save(bookmark);
    }

    @Transactional
    public void removeBookmark(Long userId, Long eventId) {
        Students student = findStudent(userId);
        Optional<StudentBookmarks> bookmarkOptional = studentBookmarksRepo
                .findByStudentEntity_StudentIdAndEvents_EventId(student.getStudentId(), eventId);
        bookmarkOptional.ifPresent(studentBookmarksRepo::delete);
    }

    @Transactional(readOnly = true)
    public List<EventResponseDTO> getStudentBookmarks(Long userId) {
        Students student = findStudent(userId);
        List<StudentBookmarks> bookmarks = studentBookmarksRepo.findByStudentEntity_StudentId(student.getStudentId());

        List<Events> events = bookmarks.stream()
                .map(StudentBookmarks::getEvents)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return EventMapper.toEventResponseDTOList(events);
    }

    @Transactional(readOnly = true)
    public boolean isEventBookmarked(Long userId, Long eventId) {
        Optional<Students> studentOptional = studentsRepo.findByUsers_UserId(userId)
                .or(() -> studentsRepo.findById(userId));
        if (studentOptional.isEmpty()) {
            return false;
        }
        return studentBookmarksRepo.existsByStudentEntity_StudentIdAndEvents_EventId(
                studentOptional.get().getStudentId(), eventId);
    }
}
