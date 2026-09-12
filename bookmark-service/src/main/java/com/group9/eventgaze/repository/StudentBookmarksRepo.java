package com.group9.eventgaze.repository;

import com.group9.eventgaze.entity.Events;
import com.group9.eventgaze.entity.StudentBookmarks;
import com.group9.eventgaze.entity.Students;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentBookmarksRepo extends JpaRepository<StudentBookmarks, Long> {

    Optional<StudentBookmarks> findByStudentEntityAndEvents(Students student, Events event);

    List<StudentBookmarks> findByStudentEntity(Students student);

    void deleteByStudentEntityAndEvents(Students student, Events event);

    boolean existsByStudentEntityAndEvents(Students student, Events event);

    Optional<StudentBookmarks> findByStudentEntity_StudentIdAndEvents_EventId(Long studentId, Long eventId);

    List<StudentBookmarks> findByStudentEntity_StudentId(Long studentId);

    void deleteByStudentEntity_StudentIdAndEvents_EventId(Long studentId, Long eventId);

    boolean existsByStudentEntity_StudentIdAndEvents_EventId(Long studentId, Long eventId);
}
