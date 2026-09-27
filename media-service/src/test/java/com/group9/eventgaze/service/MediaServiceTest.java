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
    @DisplayName("Should optimize image, stream full and thumbnail bytes to S3, and return UploadResponseDTO")
    void testUploadMedia_Success() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "banner.png", "image/png", new byte[]{1, 2, 3, 4});

        byte[] fullBytes = new byte[]{10, 20, 30};
        byte[] thumbBytes = new byte[]{5, 6};
        ImageOptimizerService.OptimizedImage fullImage = new ImageOptimizerService.OptimizedImage(
                fullBytes, 1280, 720, fullBytes.length, "image/jpeg");
        ImageOptimizerService.OptimizedImage thumbnail = new ImageOptimizerService.OptimizedImage(
                thumbBytes, 400, 225, thumbBytes.length, "image/jpeg");

        when(imageOptimizerService.optimizeImageWithThumbnail(file))
                .thenReturn(new ImageOptimizerService.MultiVariantImage(fullImage, thumbnail));
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());
        when(s3Client.utilities()).thenReturn(s3Utilities);
        when(s3Utilities.getUrl(any(Consumer.class)))
                .thenReturn(new URL("https://test-media-bucket.s3.amazonaws.com/events/mock-key.jpg"))
                .thenReturn(new URL("https://test-media-bucket.s3.amazonaws.com/events/mock-key-thumb.jpg"));

        UploadResponseDTO response = mediaService.uploadMedia(file, "events");

        assertNotNull(response);
        assertEquals("image/jpeg", response.getContentType());
        assertEquals(fullBytes.length, response.getFileSize());
        assertEquals("https://test-media-bucket.s3.amazonaws.com/events/mock-key.jpg", response.getFileUrl());
        assertTrue(response.getKey().startsWith("events/"));
        assertTrue(response.getKey().endsWith(".jpg"));
        assertFalse(response.getKey().contains("-thumb"));
        assertEquals(1280, response.getWidth());
        assertEquals(720, response.getHeight());

        // Thumbnail asserts
        assertNotNull(response.getThumbnailKey());
        assertTrue(response.getThumbnailKey().startsWith("events/"));
        assertTrue(response.getThumbnailKey().endsWith("-thumb.jpg"));
        assertEquals("https://test-media-bucket.s3.amazonaws.com/events/mock-key-thumb.jpg", response.getThumbnailUrl());
        assertEquals(400, response.getThumbWidth());
        assertEquals(225, response.getThumbHeight());
        assertEquals((long) thumbBytes.length, response.getThumbFileSize());

        ArgumentCaptor<PutObjectRequest> putRequestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client, times(2)).putObject(putRequestCaptor.capture(), any(RequestBody.class));
        var capturedRequests = putRequestCaptor.getAllValues();
        assertEquals(2, capturedRequests.size());
        assertEquals(bucketName, capturedRequests.get(0).bucket());
        assertEquals(response.getKey(), capturedRequests.get(0).key());
        assertEquals((long) fullBytes.length, capturedRequests.get(0).contentLength());
        assertEquals(response.getThumbnailKey(), capturedRequests.get(1).key());
        assertEquals((long) thumbBytes.length, capturedRequests.get(1).contentLength());
    }

    @Test
    @DisplayName("Should use default 'events' folder when folder parameter is null or empty")
    void testUploadMedia_DefaultFolderWhenBlank() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "image.jpg", "image/jpeg", new byte[]{5, 6, 7});

        byte[] fullBytes = new byte[]{8, 9};
        byte[] thumbBytes = new byte[]{1};
        ImageOptimizerService.OptimizedImage full = new ImageOptimizerService.OptimizedImage(
                fullBytes, 640, 480, fullBytes.length, "image/jpeg");
        ImageOptimizerService.OptimizedImage thumb = new ImageOptimizerService.OptimizedImage(
                thumbBytes, 400, 300, thumbBytes.length, "image/jpeg");

        when(imageOptimizerService.optimizeImageWithThumbnail(file))
                .thenReturn(new ImageOptimizerService.MultiVariantImage(full, thumb));
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());
        when(s3Client.utilities()).thenReturn(s3Utilities);
        when(s3Utilities.getUrl(any(Consumer.class)))
                .thenReturn(new URL("https://test-media-bucket.s3.amazonaws.com/events/mock-key.jpg"));

        UploadResponseDTO response = mediaService.uploadMedia(file, "   ");

        assertNotNull(response);
        assertTrue(response.getKey().startsWith("events/"));
        assertTrue(response.getKey().endsWith(".jpg"));
        assertTrue(response.getThumbnailKey().startsWith("events/"));
        assertTrue(response.getThumbnailKey().endsWith("-thumb.jpg"));
    }

    @Test
    @DisplayName("Should use custom folder prefix when specified")
    void testUploadMedia_CustomFolder() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.png", "image/png", new byte[]{1, 2});

        byte[] fullBytes = new byte[]{3, 4};
        byte[] thumbBytes = new byte[]{1};
        ImageOptimizerService.OptimizedImage full = new ImageOptimizerService.OptimizedImage(
                fullBytes, 200, 200, fullBytes.length, "image/jpeg");
        ImageOptimizerService.OptimizedImage thumb = new ImageOptimizerService.OptimizedImage(
                thumbBytes, 200, 200, thumbBytes.length, "image/jpeg");

        when(imageOptimizerService.optimizeImageWithThumbnail(file))
                .thenReturn(new ImageOptimizerService.MultiVariantImage(full, thumb));
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());
        when(s3Client.utilities()).thenReturn(s3Utilities);
        when(s3Utilities.getUrl(any(Consumer.class)))
                .thenReturn(new URL("https://test-media-bucket.s3.amazonaws.com/profiles/mock-key.jpg"));

        UploadResponseDTO response = mediaService.uploadMedia("profiles", file);

        assertNotNull(response);
        assertTrue(response.getKey().startsWith("profiles/"));
        assertTrue(response.getKey().endsWith(".jpg"));
        assertTrue(response.getThumbnailKey().startsWith("profiles/"));
        assertTrue(response.getThumbnailKey().endsWith("-thumb.jpg"));
    }

    @Test
    @DisplayName("Should wrap and throw IOException if S3Client throws S3Exception")
    void testUploadMedia_S3Exception_ThrowsIOException() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "error.jpg", "image/jpeg", new byte[]{1});

        ImageOptimizerService.OptimizedImage full = new ImageOptimizerService.OptimizedImage(
                new byte[]{1}, 100, 100, 1L, "image/jpeg");
        ImageOptimizerService.OptimizedImage thumb = new ImageOptimizerService.OptimizedImage(
                new byte[]{1}, 100, 100, 1L, "image/jpeg");

        when(imageOptimizerService.optimizeImageWithThumbnail(file))
                .thenReturn(new ImageOptimizerService.MultiVariantImage(full, thumb));
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(S3Exception.builder().message("Access Denied to S3").build());

        IOException ex = assertThrows(IOException.class, () ->
                mediaService.uploadMedia(file, "events"));

        assertTrue(ex.getMessage().contains("Failed to upload media to S3"));
    }

    @Test
    @DisplayName("Should delete both main and thumbnail objects from S3 when main key is provided")
    void testDeleteMedia_ByKey_Success() {
        mediaService.deleteMedia("events/target-key.jpg", null);

        ArgumentCaptor<DeleteObjectRequest> captor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client, times(2)).deleteObject(captor.capture());
        var keys = captor.getAllValues().stream().map(DeleteObjectRequest::key).toList();
        assertTrue(keys.contains("events/target-key.jpg"));
        assertTrue(keys.contains("events/target-key-thumb.jpg"));
    }

    @Test
    @DisplayName("Should delete both thumbnail and main objects from S3 when thumbnail key is provided")
    void testDeleteMedia_ByThumbnailKey_DeletesBoth() {
        mediaService.deleteMedia("events/target-key-thumb.jpg", null);

        ArgumentCaptor<DeleteObjectRequest> captor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client, times(2)).deleteObject(captor.capture());
        var keys = captor.getAllValues().stream().map(DeleteObjectRequest::key).toList();
        assertTrue(keys.contains("events/target-key-thumb.jpg"));
        assertTrue(keys.contains("events/target-key.jpg"));
    }

    @Test
    @DisplayName("Should extract key and delete both main and thumbnail objects from S3 when URL is provided")
    void testDeleteMedia_ByUrl_Success() {
        String url = "https://test-media-bucket.s3.us-east-1.amazonaws.com/events/url-key.jpg";
        mediaService.deleteMedia(null, url);

        ArgumentCaptor<DeleteObjectRequest> captor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client, times(2)).deleteObject(captor.capture());
        var keys = captor.getAllValues().stream().map(DeleteObjectRequest::key).toList();
        assertTrue(keys.contains("events/url-key.jpg"));
        assertTrue(keys.contains("events/url-key-thumb.jpg"));
    }

    @Test
    @DisplayName("Should verify toThumbnailKey and toMainKey conversions")
    void testKeyConversionUtilities() {
        assertEquals("events/uuid-thumb.jpg", MediaService.toThumbnailKey("events/uuid.jpg"));
        assertEquals("events/uuid-thumb.jpg", MediaService.toThumbnailKey("events/uuid-thumb.jpg"));
        assertEquals("events/uuid.jpg", MediaService.toMainKey("events/uuid-thumb.jpg"));
        assertEquals("events/uuid.jpg", MediaService.toMainKey("events/uuid.jpg"));
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
