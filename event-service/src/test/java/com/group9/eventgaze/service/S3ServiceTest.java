package com.group9.eventgaze.service;

import org.junit.jupiter.api.BeforeEach;
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
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.io.IOException;
import java.net.URL;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class S3ServiceTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private S3Utilities s3Utilities;

    private S3Service s3Service;

    private static final String TEST_BUCKET = "test-eventgaze-bucket";

    @BeforeEach
    void setUp() {
        s3Service = new S3Service(s3Client, TEST_BUCKET);
    }

    @Test
    void testUploadFileSuccessDirectStream() throws Exception {
        byte[] content = "fake image binary data".getBytes();
        MockMultipartFile file = new MockMultipartFile(
                "eventArt",
                "test-flyer.png",
                "image/png",
                content
        );

        when(s3Client.utilities()).thenReturn(s3Utilities);
        URL fakeUrl = new URL("https://" + TEST_BUCKET + ".s3.amazonaws.com/events/test-key.png");
        when(s3Utilities.getUrl(any(Consumer.class))).thenReturn(fakeUrl);
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        String resultUrl = s3Service.uploadFile("events", file);

        assertNotNull(resultUrl);
        assertEquals(fakeUrl.toString(), resultUrl);

        ArgumentCaptor<PutObjectRequest> putRequestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        ArgumentCaptor<RequestBody> requestBodyCaptor = ArgumentCaptor.forClass(RequestBody.class);

        verify(s3Client).putObject(putRequestCaptor.capture(), requestBodyCaptor.capture());

        PutObjectRequest capturedRequest = putRequestCaptor.getValue();
        assertEquals(TEST_BUCKET, capturedRequest.bucket());
        assertEquals("image/png", capturedRequest.contentType());
        assertTrue(capturedRequest.key().startsWith("events/"));
        assertTrue(capturedRequest.key().endsWith("test-flyer.png"));

        RequestBody capturedBody = requestBodyCaptor.getValue();
        assertEquals(content.length, capturedBody.contentLength());
    }

    @Test
    void testUploadFileEmptyThrowsException() {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "eventArt",
                "empty.png",
                "image/png",
                new byte[0]
        );

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                s3Service.uploadFile("events", emptyFile)
        );
        assertTrue(ex.getMessage().contains("empty or null"));
    }

    @Test
    void testUploadFileNullThrowsException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                s3Service.uploadFile("events", null)
        );
        assertTrue(ex.getMessage().contains("empty or null"));
    }

    @Test
    void testUploadFileInvalidMimeTypeThrowsException() {
        MockMultipartFile pdfFile = new MockMultipartFile(
                "eventArt",
                "document.pdf",
                "application/pdf",
                "pdf content".getBytes()
        );

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                s3Service.uploadFile("events", pdfFile)
        );
        assertTrue(ex.getMessage().contains("Invalid file type"));
    }

    @Test
    void testUploadFileInvalidExtensionThrowsException() {
        MockMultipartFile exeFile = new MockMultipartFile(
                "eventArt",
                "malicious.exe",
                "image/png", // spoofed mime type
                "some binary".getBytes()
        );

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                s3Service.uploadFile("events", exeFile)
        );
        assertTrue(ex.getMessage().contains("Invalid file extension"));
    }

    @Test
    void testGenerateUniqueKeySanitization() {
        MockMultipartFile fileWithSpacesAndSpecialChars = new MockMultipartFile(
                "eventArt",
                "../../hack & test (1).jpeg",
                "image/jpeg",
                "data".getBytes()
        );

        String key = s3Service.generateUniqueKey("events", fileWithSpacesAndSpecialChars);

        assertTrue(key.startsWith("events/"));
        assertFalse(key.contains(".."));
        assertFalse(key.contains("&"));
        assertFalse(key.contains(" "));
        assertTrue(key.endsWith(".jpeg"));
    }

    @Test
    void testGenerateUniqueKeyWhenFolderIncludesFilename() {
        MockMultipartFile file = new MockMultipartFile(
                "eventArt",
                "poster.jpg",
                "image/jpeg",
                "data".getBytes()
        );

        // Backward compatibility: caller passes "events/poster.jpg"
        String key = s3Service.generateUniqueKey("events/poster.jpg", file);

        assertTrue(key.startsWith("events/"));
        assertTrue(key.endsWith("-poster.jpg"));
        assertFalse(key.startsWith("events/poster.jpg/"));
    }

    @Test
    void testDeleteFile() {
        when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                .thenReturn(DeleteObjectResponse.builder().build());

        s3Service.deleteFile("events/some-image.jpg");

        ArgumentCaptor<DeleteObjectRequest> captor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(captor.capture());
        assertEquals(TEST_BUCKET, captor.getValue().bucket());
        assertEquals("events/some-image.jpg", captor.getValue().key());
    }

    @Test
    void testExtractKeyFromUrl_virtualHostedStandard() {
        String url = "https://" + TEST_BUCKET + ".s3.amazonaws.com/events/uuid-poster.png";
        String key = s3Service.extractKeyFromUrl(url);
        assertEquals("events/uuid-poster.png", key);
    }

    @Test
    void testExtractKeyFromUrl_virtualHostedWithRegion() {
        String url = "https://" + TEST_BUCKET + ".s3.us-west-2.amazonaws.com/events/special/flyer.jpeg";
        String key = s3Service.extractKeyFromUrl(url);
        assertEquals("events/special/flyer.jpeg", key);
    }

    @Test
    void testExtractKeyFromUrl_pathStyle() {
        String url = "https://s3.amazonaws.com/" + TEST_BUCKET + "/events/image.png";
        String key = s3Service.extractKeyFromUrl(url);
        assertEquals("events/image.png", key);

        String regionalPathStyle = "https://s3.eu-central-1.amazonaws.com/" + TEST_BUCKET + "/events/image.png";
        assertEquals("events/image.png", s3Service.extractKeyFromUrl(regionalPathStyle));
    }

    @Test
    void testExtractKeyFromUrl_alreadyRawKey() {
        assertEquals("events/direct-key.png", s3Service.extractKeyFromUrl("events/direct-key.png"));
        assertEquals("events/direct-key.png", s3Service.extractKeyFromUrl("/events/direct-key.png"));
    }

    @Test
    void testExtractKeyFromUrl_withQueryParams() {
        String url = "https://" + TEST_BUCKET + ".s3.amazonaws.com/events/key.png?AWSAccessKeyId=123&Signature=abc";
        assertEquals("events/key.png", s3Service.extractKeyFromUrl(url));
    }

    @Test
    void testExtractKeyFromUrl_urlDecoded() {
        String url = "https://" + TEST_BUCKET + ".s3.amazonaws.com/events/my%20test%20flyer.png";
        assertEquals("events/my test flyer.png", s3Service.extractKeyFromUrl(url));
    }

    @Test
    void testExtractKeyFromUrl_nullOrBlank() {
        assertNull(s3Service.extractKeyFromUrl(null));
        assertNull(s3Service.extractKeyFromUrl(""));
        assertNull(s3Service.extractKeyFromUrl("   "));
    }

    @Test
    void testIsDefaultPlaceholder() {
        assertTrue(s3Service.isDefaultPlaceholder("default.png"));
        assertTrue(s3Service.isDefaultPlaceholder("https://bucket.s3.amazonaws.com/events/default-poster.png"));
        assertTrue(s3Service.isDefaultPlaceholder("https://bucket.s3.amazonaws.com/placeholder.png"));
        assertTrue(s3Service.isDefaultPlaceholder("https://via.placeholder.com/300x200"));
        assertTrue(s3Service.isDefaultPlaceholder("/images/default_banner.jpg"));

        assertFalse(s3Service.isDefaultPlaceholder("events/7cfb901d-real-flyer.png"));
        assertFalse(s3Service.isDefaultPlaceholder("https://" + TEST_BUCKET + ".s3.amazonaws.com/events/cool-concert.png"));
        assertFalse(s3Service.isDefaultPlaceholder(null));
        assertFalse(s3Service.isDefaultPlaceholder(""));
    }

    @Test
    void testDeleteFile_skipsPlaceholder() {
        s3Service.deleteFile("events/default-image.png");
        s3Service.deleteFile("placeholder.jpg");
        s3Service.deleteFile("");
        s3Service.deleteFile(null);

        verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    void testDeleteFile_handlesExceptionGracefully() {
        when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                .thenThrow(new RuntimeException("Simulated S3 network failure"));

        assertDoesNotThrow(() -> s3Service.deleteFile("events/valid-image.png"));
        verify(s3Client).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    void testDeleteFileByUrl() {
        when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                .thenReturn(DeleteObjectResponse.builder().build());

        String url = "https://" + TEST_BUCKET + ".s3.amazonaws.com/events/sample-art.png";
        s3Service.deleteFileByUrl(url);

        ArgumentCaptor<DeleteObjectRequest> captor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(captor.capture());
        assertEquals(TEST_BUCKET, captor.getValue().bucket());
        assertEquals("events/sample-art.png", captor.getValue().key());
    }
}
