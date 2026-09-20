package com.group9.eventgaze.controller;

import com.group9.eventgaze.config.JwtAuthenticationFilter;
import com.group9.eventgaze.config.SecurityConfig;
import com.group9.eventgaze.entity.dto.UploadResponseDTO;
import com.group9.eventgaze.service.MediaService;
import com.group9.eventgaze.utils.JwtTokenUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MediaController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class MediaControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MediaService mediaService;

    @MockBean
    private JwtTokenUtil jwtTokenUtil;

    @Test
    @DisplayName("Unauthenticated upload request should be rejected with 401 Unauthorized")
    void testUploadMedia_Unauthenticated_Returns401() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "poster.jpg", "image/jpeg", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/eventgaze/media/upload")
                        .file(file)
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Authenticated user upload request should be permitted")
    @WithMockUser(username = "1", roles = {"PUBLISHER"})
    void testUploadMedia_Authenticated_Success() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "poster.jpg", "image/jpeg", new byte[]{1, 2, 3});

        UploadResponseDTO responseDTO = UploadResponseDTO.builder()
                .fileUrl("https://bucket.s3.amazonaws.com/events/uuid.jpg")
                .key("events/uuid.jpg")
                .fileSize(100L)
                .contentType("image/jpeg")
                .build();

        when(mediaService.uploadMedia(any(), eq("events"))).thenReturn(responseDTO);

        mockMvc.perform(multipart("/eventgaze/media/upload")
                        .file(file)
                        .param("folder", "events")
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Unauthenticated delete request should be rejected with 401 Unauthorized")
    void testDeleteMedia_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(delete("/eventgaze/media/delete")
                        .param("key", "events/test.jpg")
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Authenticated user delete request should be permitted")
    @WithMockUser(username = "1", roles = {"PUBLISHER"})
    void testDeleteMedia_Authenticated_Success() throws Exception {
        mockMvc.perform(delete("/eventgaze/media/delete")
                        .param("key", "events/test.jpg")
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }
}
