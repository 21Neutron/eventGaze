package com.group9.eventgaze.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.group9.eventgaze.config.GlobalExceptionHandler;
import com.group9.eventgaze.entity.dto.UploadResponseDTO;
import com.group9.eventgaze.service.MediaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MediaControllerTest {

    @Mock
    private MediaService mediaService;

    @InjectMocks
    private MediaController mediaController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(mediaController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("POST /eventgaze/media/upload should successfully upload, optimize, and return UploadResponseDTO")
    void testUploadMediaSuccess() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "poster.jpg", "image/jpeg", new byte[]{1, 2, 3, 4});

        UploadResponseDTO responseDTO = UploadResponseDTO.builder()
                .fileUrl("https://bucket.s3.amazonaws.com/events/uuid.jpg")
                .key("events/uuid.jpg")
                .fileSize(184520L)
                .contentType("image/jpeg")
                .build();

        when(mediaService.uploadMedia(any(), eq("events"))).thenReturn(responseDTO);

        mockMvc.perform(multipart("/eventgaze/media/upload")
                        .file(file)
                        .param("folder", "events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileUrl").value("https://bucket.s3.amazonaws.com/events/uuid.jpg"))
                .andExpect(jsonPath("$.key").value("events/uuid.jpg"))
                .andExpect(jsonPath("$.fileSize").value(184520))
                .andExpect(jsonPath("$.contentType").value("image/jpeg"));

        verify(mediaService).uploadMedia(any(), eq("events"));
    }

    @Test
    @DisplayName("POST /eventgaze/media/upload with invalid file should return 400 Bad Request")
    void testUploadMediaValidationError() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", "empty.jpg", "image/jpeg", new byte[0]);

        doThrow(new IllegalArgumentException("Cannot upload an empty or null file."))
                .when(mediaService).uploadMedia(any(), any());

        mockMvc.perform(multipart("/eventgaze/media/upload")
                        .file(emptyFile))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Cannot upload an empty or null file."));
    }

    @Test
    @DisplayName("DELETE /eventgaze/media/delete with key should return 204 No Content")
    void testDeleteMediaEndpointWithKey() throws Exception {
        doNothing().when(mediaService).deleteMedia("events/target.jpg", null);

        mockMvc.perform(delete("/eventgaze/media/delete")
                        .param("key", "events/target.jpg"))
                .andExpect(status().isNoContent());

        verify(mediaService).deleteMedia("events/target.jpg", null);
    }

    @Test
    @DisplayName("DELETE /eventgaze/media/delete with url should return 204 No Content")
    void testDeleteMediaEndpointWithUrl() throws Exception {
        String url = "https://bucket.s3.amazonaws.com/events/target.jpg";
        doNothing().when(mediaService).deleteMedia(null, url);

        mockMvc.perform(delete("/eventgaze/media/delete")
                        .param("url", url))
                .andExpect(status().isNoContent());

        verify(mediaService).deleteMedia(null, url);
    }

    @Test
    @DisplayName("DELETE /eventgaze/media (root) with key should return 204 No Content")
    void testDeleteMediaRootEndpoint() throws Exception {
        doNothing().when(mediaService).deleteMedia("events/root.jpg", null);

        mockMvc.perform(delete("/eventgaze/media")
                        .param("key", "events/root.jpg"))
                .andExpect(status().isNoContent());

        verify(mediaService).deleteMedia("events/root.jpg", null);
    }

    @Test
    @DisplayName("DELETE /eventgaze/media/delete missing key and url returns 400 Bad Request")
    void testDeleteMediaMissingParametersReturnsBadRequest() throws Exception {
        doThrow(new IllegalArgumentException("Either 'key' or 'url' must be provided for deletion."))
                .when(mediaService).deleteMedia(null, null);

        mockMvc.perform(delete("/eventgaze/media/delete"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Either 'key' or 'url' must be provided for deletion."));
    }
}
