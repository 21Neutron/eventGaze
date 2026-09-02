package com.group9.eventgaze.service;

import com.group9.eventgaze.entity.Publishers;
import com.group9.eventgaze.entity.Users;
import com.group9.eventgaze.repository.PublishersRepo;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PublishersService {

    @Autowired
    private PublishersRepo publishersRepo;

    public PublishersService() {
    }

    public PublishersService(PublishersRepo publishersRepo) {
        this.publishersRepo = publishersRepo;
    }

    public Publishers findByIdOrThrow(Long myId) {
        return publishersRepo.findById(myId).orElseThrow(
                () -> new EntityNotFoundException("Publisher with " + myId + " not found.")
        );
    }

    public Publishers findByUser(Users user) {
        return publishersRepo.findByUsers(user)
                .orElseThrow(() -> new EntityNotFoundException("Publisher not found for user: " + user.getUserId()));
    }
}
