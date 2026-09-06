package com.group9.eventgaze.service;

import com.group9.eventgaze.entity.EventCategory;
import com.group9.eventgaze.repository.EventCategoryRepo;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class EventCategoryService {

    @Autowired
    private EventCategoryRepo eventCategoryRepo;

    public List<EventCategory>getAllCategory(){
        return eventCategoryRepo.findAll();
    }

    public Optional<EventCategory> findById(Long myId){
        return eventCategoryRepo.findById(myId);
    }

    public EventCategory findByIdOrThrow(Long myId) {
        return eventCategoryRepo.findById(myId).orElseThrow(
                () -> new EntityNotFoundException("Event category with ID " + myId + " not found.")
        );
    }
}
