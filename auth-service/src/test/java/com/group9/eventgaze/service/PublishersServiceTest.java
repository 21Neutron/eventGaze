package com.group9.eventgaze.service;

import com.group9.eventgaze.entity.Publishers;
import com.group9.eventgaze.entity.Users;
import com.group9.eventgaze.repository.PublishersRepo;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PublishersServiceTest {

    @Mock
    private PublishersRepo publishersRepo;

    @InjectMocks
    private PublishersService publishersService;

    @Test
    @DisplayName("findByIdOrThrow: Returns publisher when found")
    void testFindByIdOrThrow_Found() {
        Publishers publisher = new Publishers();
        publisher.setPublisherId(1L);
        when(publishersRepo.findById(1L)).thenReturn(Optional.of(publisher));

        Publishers result = publishersService.findByIdOrThrow(1L);
        assertNotNull(result);
        assertEquals(1L, result.getPublisherId());
    }

    @Test
    @DisplayName("findByIdOrThrow: Throws EntityNotFoundException when not found")
    void testFindByIdOrThrow_NotFound() {
        when(publishersRepo.findById(99L)).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class, () ->
                publishersService.findByIdOrThrow(99L));
        assertTrue(ex.getMessage().contains("99"));
    }

    @Test
    @DisplayName("findByUser: Returns publisher when user matches")
    void testFindByUser_Found() {
        Users user = new Users();
        user.setUserId(5L);
        Publishers publisher = new Publishers();
        publisher.setPublisherId(10L);
        publisher.setUsers(user);

        when(publishersRepo.findByUsers(user)).thenReturn(Optional.of(publisher));

        Publishers result = publishersService.findByUser(user);
        assertNotNull(result);
        assertEquals(10L, result.getPublisherId());
    }

    @Test
    @DisplayName("findByUser: Throws EntityNotFoundException when user not matched")
    void testFindByUser_NotFound() {
        Users user = new Users();
        user.setUserId(5L);

        when(publishersRepo.findByUsers(user)).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class, () ->
                publishersService.findByUser(user));
        assertTrue(ex.getMessage().contains("5"));
    }
}
