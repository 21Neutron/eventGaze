package com.group9.eventgaze.controller;

import com.group9.eventgaze.config.GlobalExceptionHandler;
import com.group9.eventgaze.entity.Events;
import com.group9.eventgaze.service.EventCategoryService;
import com.group9.eventgaze.service.EventsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class EventsSearchFilterControllerTest {

    @Mock
    private EventsService eventsService;

    @Mock
    private EventCategoryService eventCategoryService;

    @InjectMocks
    private EventsController eventsController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(eventsController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void testSearchEndpointWithQueryParams() throws Exception {
        Events event = new Events();
        event.setEventId(10L);
        event.setEventName("Spring Coding Contest");
        event.setEventDescription("Compete with coders");
        event.setEventTags("coding,java");
        event.setEventDate(LocalDate.of(2026, 12, 1));

        Page<Events> page = new PageImpl<>(List.of(event), PageRequest.of(0, 10), 1);
        when(eventsService.searchEvents(eq("coding"), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/eventgaze/events/search")
                        .param("query", "coding")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sortBy", "eventId")
                        .param("direction", "ASC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].eventId").value(10))
                .andExpect(jsonPath("$.content[0].eventName").value("Spring Coding Contest"))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(eventsService).searchEvents(eq("coding"), any(Pageable.class));
    }

    @Test
    void testSearchEndpointWithDefaultParams() throws Exception {
        Page<Events> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
        when(eventsService.searchEvents(eq(""), any(Pageable.class))).thenReturn(emptyPage);

        mockMvc.perform(get("/eventgaze/events/search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        verify(eventsService).searchEvents(eq(""), any(Pageable.class));
    }

    @Test
    void testUpcomingEndpoint() throws Exception {
        Events event = new Events();
        event.setEventId(20L);
        event.setEventName("Upcoming Gala");
        event.setEventDate(LocalDate.now().plusDays(7));

        Page<Events> page = new PageImpl<>(List.of(event), PageRequest.of(0, 5), 1);
        when(eventsService.getUpcomingEvents(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/eventgaze/events/upcoming")
                        .param("page", "0")
                        .param("size", "5")
                        .param("sortBy", "eventDate")
                        .param("direction", "ASC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].eventId").value(20))
                .andExpect(jsonPath("$.content[0].eventName").value("Upcoming Gala"))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(eventsService).getUpcomingEvents(any(Pageable.class));
    }

    @Test
    void testPastEndpoint() throws Exception {
        Events event = new Events();
        event.setEventId(30L);
        event.setEventName("Past Exhibition");
        event.setEventDate(LocalDate.now().minusDays(30));

        Page<Events> page = new PageImpl<>(List.of(event), PageRequest.of(0, 5), 1);
        when(eventsService.getPastEvents(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/eventgaze/events/past")
                        .param("page", "0")
                        .param("size", "5")
                        .param("sortBy", "eventDate")
                        .param("direction", "DESC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].eventId").value(30))
                .andExpect(jsonPath("$.content[0].eventName").value("Past Exhibition"))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(eventsService).getPastEvents(any(Pageable.class));
    }
}
