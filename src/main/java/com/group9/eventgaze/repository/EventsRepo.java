package com.group9.eventgaze.repository;

import com.group9.eventgaze.entity.Events;
import com.group9.eventgaze.entity.Publishers;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventsRepo extends JpaRepository<Events, Long> {

    List<Events> findByEventCategory_CategoryId(Long categoryId);

    Page<Events> findByEventCategory_CategoryId(Long categoryId, Pageable pageable);

    List<Events> findByCollege_CollegeId(Long collegeId);

    Page<Events> findByCollege_CollegeId(Long collegeId, Pageable pageable);

    List<Events> findByPublishers(Publishers publishers);

    Page<Events> findByPublishers(Publishers publishers, Pageable pageable);

    List<Events> findByPublishers_PublisherId(Long publisherId);

    Page<Events> findByPublishers_PublisherId(Long publisherId, Pageable pageable);
}
