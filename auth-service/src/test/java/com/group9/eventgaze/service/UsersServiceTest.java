package com.group9.eventgaze.service;

import com.group9.eventgaze.entity.Users;
import com.group9.eventgaze.repository.UsersRepo;
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
class UsersServiceTest {

    @Mock
    private UsersRepo usersRepo;

    @InjectMocks
    private UsersService usersService;

    @Test
    @DisplayName("findByIdOrThrow: Returns user when found")
    void testFindByIdOrThrow_Found() {
        Users user = new Users();
        user.setUserId(1L);
        when(usersRepo.findById(1L)).thenReturn(Optional.of(user));

        Users result = usersService.findByIdOrThrow(1L);
        assertNotNull(result);
        assertEquals(1L, result.getUserId());
    }

    @Test
    @DisplayName("findByIdOrThrow: Throws EntityNotFoundException when not found")
    void testFindByIdOrThrow_NotFound() {
        when(usersRepo.findById(99L)).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class, () ->
                usersService.findByIdOrThrow(99L));
        assertTrue(ex.getMessage().contains("99"));
    }
}
