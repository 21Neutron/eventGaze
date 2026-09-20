package com.group9.eventgaze.service;

import com.group9.eventgaze.entity.dto.UploadResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Utilities;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.net.URL;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MediaServiceTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private ImageOptimizerService imageOptimizerService;

    @Mock
    private S3Utilities s3Utilities;

    private MediaService mediaService;
    private final String bucketName = "test-media-bucket";

    @BeforeEach
    void setUp() {
        mediaService = new MediaService(imageOptimizerService, s3Client, bucketName);
    }

    @Test
    @DisplayName("Should optimize image, stream bytes to S3 and return UploadResponseDTO")
    void testUploadMedia_Success() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "banner.png", "image/png", new byte[]{1, 2, 3, 4});

        byte[] compressedBytes = new byte[]{10, 20, 30};
        ImageOptimizerService.OptimizedImage optimized = new ImageOptimizerService.OptimizedImage(
                compressedBytes, 1280, 720, compressedBytes.length, "image/jpeg");

        when(imageOptimizerService.optimizeImage(file)).thenReturn(optimized);
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());
        when(s3Client.utilities()).thenReturn(s3Utilities);
        when(s3Utilities.getUrl(any(Consumer.class)))
                .thenReturn(new URL("https://test-media-bucket.s3.amazonaws.com/events/mock-key.jpg"));

        UploadResponseDTO response = mediaService.uploadMedia(file, "events");

        assertNotNull(response);
        assertEquals("image/jpeg", response.getContentType());
        assertEquals(compressedBytes.length, response.getFileSize());
        assertEquals("https://test-media-bucket.s3.amazonaws.com/events/mock-key.jpg", response.getFileUrl());
        assertTrue(response.getKey().startsWith("events/"));
        assertTrue(response.getKey().endsWith(".jpg"));

        ArgumentCaptor<PutObjectRequest> putRequestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(putRequestCaptor.capture(), any(RequestBody.class));
        PutObjectRequest capturedRequest = putRequestCaptor.getValue();
        assertEquals(bucketName, capturedRequest.bucket());
        assertEquals("image/jpeg", capturedRequest.contentType());
        assertEquals((long) compressedBytes.length, capturedRequest.contentLength());
    }

    @Test
    @DisplayName("Should use default 'events' folder when folder parameter is null or empty")
    void testUploadMedia_DefaultFolderWhenBlank() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "image.jpg", "image/jpeg", new byte[]{5, 6, 7});

        byte[] compressed = new byte[]{8, 9};
        ImageOptimizerService.OptimizedImage optimized = new ImageOptimizerService.OptimizedImage(
                compressed, 640, 480, compressed.length, "image/jpeg");

        when(imageOptimizerService.optimizeImage(file)).thenReturn(optimized);
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());
        when(s3Client.utilities()).thenReturn(s3Utilities);
        when(s3Utilities.getUrl(any(Consumer.class)))
                .thenReturn(new URL("https://test-media-bucket.s3.amazonaws.com/events/mock-key.jpg"));

        UploadResponseDTO response = mediaService.uploadMedia(file, "   ");

        assertNotNull(response);
        assertTrue(response.getKey().startsWith("events/"));
        assertTrue(response.getKey().endsWith(".jpg"));
    }

    @Test
    @DisplayName("Should use custom folder prefix when specified")
    void testUploadMedia_CustomFolder() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.png", "image/png", new byte[]{1, 2});

        byte[] compressed = new byte[]{3, 4};
        ImageOptimizerService.OptimizedImage optimized = new ImageOptimizerService.OptimizedImage(
                compressed, 200, 200, compressed.length, "image/jpeg");

        when(imageOptimizerService.optimizeImage(file)).thenReturn(optimized);
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());
        when(s3Client.utilities()).thenReturn(s3Utilities);
        when(s3Utilities.getUrl(any(Consumer.class)))
                .thenReturn(new URL("https://test-media-bucket.s3.amazonaws.com/profiles/mock-key.jpg"));

        UploadResponseDTO response = mediaService.uploadMedia(file, "profiles");

        assertNotNull(response);
        assertTrue(response.getKey().startsWith("profiles/"));
        assertTrue(response.getKey().endsWith(".jpg"));
    }

    @Test
    @DisplayName("Should wrap and throw IOException if S3Client throws S3Exception")
    void testUploadMedia_S3Exception_ThrowsIOException() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "error.jpg", "image/jpeg", new byte[]{1});

        ImageOptimizerService.OptimizedImage optimized = new ImageOptimizerService.OptimizedImage(
                new byte[]{1}, 100, 100, 1L, "image/jpeg");

        when(imageOptimizerService.optimizeImage(file)).thenReturn(optimized);
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(S3Exception.builder().message("Access Denied to S3").build());

        IOException ex = assertThrows(IOException.class, () ->
                mediaService.uploadMedia(file, "events"));

        assertTrue(ex.getMessage().contains("Failed to upload media to S3"));
    }

    @Test
    @DisplayName("Should delete object from S3 when key is provided")
    void testDeleteMedia_ByKey_Success() {
        mediaService.deleteMedia("events/target-key.jpg", null);

        ArgumentCaptor<DeleteObjectRequest> captor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(captor.capture());
        DeleteObjectRequest req = captor.getValue();
        assertEquals(bucketName, req.bucket());
        assertEquals("events/target-key.jpg", req.key());
    }

    @Test
    @DisplayName("Should extract key and delete object from S3 when URL is provided")
    void testDeleteMedia_ByUrl_Success() {
        String url = "https://test-media-bucket.s3.us-east-1.amazonaws.com/events/url-key.jpg";
        mediaService.deleteMedia(null, url);

        ArgumentCaptor<DeleteObjectRequest> captor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(captor.capture());
        assertEquals("events/url-key.jpg", captor.getValue().key());
    }

    @Test
    @DisplayName("Should skip deletion when asset is a default placeholder image")
    void testDeleteMedia_Placeholder_SkipsDeletion() {
        mediaService.deleteMedia(null, "https://example.com/assets/default-event.png");
        mediaService.deleteMedia("placeholder.jpg", null);
        mediaService.deleteMedia(null, "https://picsum.photos/200/300");

        verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when both key and url are null/empty")
    void testDeleteMedia_MissingKeyAndUrl_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () ->
                mediaService.deleteMedia(null, null));
        assertThrows(IllegalArgumentException.class, () ->
                mediaService.deleteMedia("", "   "));
    }

    @Test
    @DisplayName("Should extract key accurately from various S3 URL formats")
    void testExtractKeyFromUrl_Formats() {
        // Virtual-hosted style with region
        assertEquals("events/test.jpg", mediaService.extractKeyFromUrl(
                "https://test-media-bucket.s3.us-east-1.amazonaws.com/events/test.jpg"));

        // Virtual-hosted style without region
        assertEquals("events/test.jpg", mediaService.extractKeyFromUrl(
                "https://test-media-bucket.s3.amazonaws.com/events/test.jpg"));

        // Path-style
        assertEquals("events/test.jpg", mediaService.extractKeyFromUrl(
                "https://s3.amazonaws.com/test-media-bucket/events/test.jpg"));

        // Raw key
        assertEquals("events/test.jpg", mediaService.extractKeyFromUrl("events/test.jpg"));
        assertEquals("events/test.jpg", mediaService.extractKeyFromUrl("/events/test.jpg"));
    }
}
