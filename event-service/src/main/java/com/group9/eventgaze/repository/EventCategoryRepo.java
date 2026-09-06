package com.group9.eventgaze.repository;

import com.group9.eventgaze.entity.EventCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

@Repository
public interface EventCategoryRepo extends JpaRepository<EventCategory,Long> {

}
