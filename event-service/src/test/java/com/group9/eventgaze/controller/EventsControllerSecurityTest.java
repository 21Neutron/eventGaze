package com.group9.eventgaze.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.group9.eventgaze.config.GlobalExceptionHandler;
import com.group9.eventgaze.entity.Events;
import com.group9.eventgaze.entity.Publishers;
import com.group9.eventgaze.entity.Users;
import com.group9.eventgaze.entity.dto.CustomUserDetails;
import com.group9.eventgaze.entity.dto.EventRequestDTO;
import com.group9.eventgaze.entity.dto.EventResponseDTO;
import com.group9.eventgaze.service.EventCategoryService;
import com.group9.eventgaze.service.EventsService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class EventsControllerSecurityTest {

    @Mock
    private EventsService eventsService;

    @Mock
    private EventCategoryService eventCategoryService;

    @InjectMocks
    private EventsController eventsController;

    private MockMvc mockMvc;
    private CustomUserDetails publisherDetails;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        publisherDetails = new CustomUserDetails(100L, "PUBLISHER");
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(publisherDetails, null, publisherDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        mockMvc = MockMvcBuilders.standaloneSetup(eventsController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        objectMapper = new ObjectMapper();
    }

    @Test
    void testCreateEventSuccess() throws Exception {
        doNothing().when(eventsService).createEvent(any(EventRequestDTO.class));

        mockMvc.perform(post("/eventgaze/events/create")
                        .param("eventName", "Tech Conference")
                        .param("eventDescription", "Annual conference")
                        .param("eventDate", "15-12-2026")
                        .param("eventCategoryId", "1")
                        .param("colleges", "2"))
                .andExpect(status().isCreated());

        verify(eventsService).createEvent(any(EventRequestDTO.class));
    }

    @Test
    void testCreateEventJsonWithArtworkUrlSuccess() throws Exception {
        doNothing().when(eventsService).createEvent(any(EventRequestDTO.class));

        String jsonPayload = """
                {
                    "eventName": "AI Symposium 2026",
                    "eventDescription": "Annual artificial intelligence symposium",
                    "eventDate": "20-12-2026",
                    "eventCategoryId": 1,
                    "colleges": 2,
                    "eventArt": "https://bucket.s3.us-east-1.amazonaws.com/events/ai-symposium.jpg"
                }
                """;

        mockMvc.perform(post("/eventgaze/events/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isCreated());

        verify(eventsService).createEvent(argThat(dto ->
                dto.getEventName().equals("AI Symposium 2026") &&
                "https://bucket.s3.us-east-1.amazonaws.com/events/ai-symposium.jpg".equals(dto.getEventArtUrl())
        ));
    }

    @Test
    void testCreateEventAccessDeniedReturns403() throws Exception {
        doThrow(new AccessDeniedException("User is not authenticated"))
                .when(eventsService).createEvent(any(EventRequestDTO.class));

        mockMvc.perform(post("/eventgaze/events/create")
                        .param("eventName", "Tech Conference"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("User is not authenticated"));
    }

    @Test
    void testDeleteEventByIdSuccess() throws Exception {
        doNothing().when(eventsService).deleteEventById(1L);

        mockMvc.perform(delete("/eventgaze/events/id/1"))
                .andExpect(status().isNoContent());

        verify(eventsService).deleteEventById(1L);
    }

    @Test
    void testDeleteEventByIdNotFoundReturns404() throws Exception {
        doThrow(new EntityNotFoundException("Event with ID 999 not found."))
                .when(eventsService).deleteEventById(999L);

        mockMvc.perform(delete("/eventgaze/events/id/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Event with ID 999 not found."));
    }

    @Test
    void testDeleteEventByIdNotOwnerReturns403() throws Exception {
        doThrow(new AccessDeniedException("You do not have permission to delete this event"))
                .when(eventsService).deleteEventById(1L);

        mockMvc.perform(delete("/eventgaze/events/id/1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("You do not have permission to delete this event"));
    }

    @Test
    void testEditByEventIdSuccess() throws Exception {
        Events updatedEvent = new Events();
        updatedEvent.setEventId(1L);
        updatedEvent.setEventName("Updated Name");

        when(eventsService.updateEventById(eq(1L), any(Events.class))).thenReturn(updatedEvent);

        mockMvc.perform(put("/eventgaze/events/eventEdit/id/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedEvent)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventName").value("Updated Name"));

        verify(eventsService).updateEventById(eq(1L), any(Events.class));
    }

    @Test
    void testEditByEventIdNotOwnerReturns403() throws Exception {
        Events eventToUpdate = new Events();
        eventToUpdate.setEventName("Hackathon");

        doThrow(new AccessDeniedException("You do not have permission to edit this event"))
                .when(eventsService).updateEventById(eq(1L), any(Events.class));

        mockMvc.perform(put("/eventgaze/events/eventEdit/id/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(eventToUpdate)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("You do not have permission to edit this event"));
    }

    @Test
    void testEditByEventIdNotFoundReturns404() throws Exception {
        Events eventToUpdate = new Events();
        eventToUpdate.setEventName("Hackathon");

        doThrow(new EntityNotFoundException("Event with ID 999 not found."))
                .when(eventsService).updateEventById(eq(999L), any(Events.class));

        mockMvc.perform(put("/eventgaze/events/eventEdit/id/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(eventToUpdate)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }
}
