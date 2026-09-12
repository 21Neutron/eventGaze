package com.group9.eventgaze.controller;

import com.group9.eventgaze.entity.dto.CustomUserDetails;
import com.group9.eventgaze.entity.dto.EventResponseDTO;
import com.group9.eventgaze.service.BookmarkService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class BookmarkControllerTest {

    @Mock
    private BookmarkService bookmarkService;

    @InjectMocks
    private BookmarkController bookmarkController;

    private MockMvc mockMvc;
    private CustomUserDetails mockUserDetails;

    @BeforeEach
    void setUp() {
        mockUserDetails = new CustomUserDetails(15L, "STUDENT");

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(mockUserDetails, null, mockUserDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        HandlerMethodArgumentResolver authPrincipalResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.getParameterType().equals(CustomUserDetails.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                return mockUserDetails;
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(bookmarkController)
                .setCustomArgumentResolvers(authPrincipalResolver)
                .build();
    }

    @Test
    void testAddBookmark() throws Exception {
        doNothing().when(bookmarkService).addBookmark(15L, 42L);

        mockMvc.perform(post("/eventgaze/bookmarks/42"))
                .andExpect(status().isOk());

        verify(bookmarkService).addBookmark(15L, 42L);
    }

    @Test
    void testRemoveBookmark() throws Exception {
        doNothing().when(bookmarkService).removeBookmark(15L, 42L);

        mockMvc.perform(delete("/eventgaze/bookmarks/42"))
                .andExpect(status().isOk());

        verify(bookmarkService).removeBookmark(15L, 42L);
    }

    @Test
    void testGetStudentBookmarks() throws Exception {
        EventResponseDTO eventDto = new EventResponseDTO();
        eventDto.setEventId(42L);
        eventDto.setEventName("Design Sprint");

        when(bookmarkService.getStudentBookmarks(15L)).thenReturn(List.of(eventDto));

        mockMvc.perform(get("/eventgaze/bookmarks/my-bookmarks")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventId").value(42L))
                .andExpect(jsonPath("$[0].eventName").value("Design Sprint"));

        verify(bookmarkService).getStudentBookmarks(15L);
    }

    @Test
    void testIsEventBookmarkedTrue() throws Exception {
        when(bookmarkService.isEventBookmarked(15L, 42L)).thenReturn(true);

        mockMvc.perform(get("/eventgaze/bookmarks/check/42"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        verify(bookmarkService).isEventBookmarked(15L, 42L);
    }

    @Test
    void testIsEventBookmarkedFalse() throws Exception {
        when(bookmarkService.isEventBookmarked(15L, 42L)).thenReturn(false);

        mockMvc.perform(get("/eventgaze/bookmarks/check/42"))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));

        verify(bookmarkService).isEventBookmarked(15L, 42L);
    }
}
