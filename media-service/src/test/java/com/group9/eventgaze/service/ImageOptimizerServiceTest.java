package com.group9.eventgaze.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class ImageOptimizerServiceTest {

    private ImageOptimizerService optimizerService;

    @BeforeEach
    void setUp() {
        optimizerService = new ImageOptimizerService();
    }

    private byte[] createTestImageBytes(int width, int height, String format, boolean hasAlpha) throws IOException {
        int imageType = hasAlpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        BufferedImage image = new BufferedImage(width, height, imageType);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.MAGENTA);
        g.fillRect(0, 0, width, height);
        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, format, baos);
        return baos.toByteArray();
    }

    @Test
    @DisplayName("Should optimize and keep dimensions when image is within 1280x1280 bounds")
    void testOptimizeImage_WithinBounds_MaintainsDimensions() throws IOException {
        byte[] imageBytes = createTestImageBytes(800, 600, "jpg", false);
        MockMultipartFile file = new MockMultipartFile(
                "file", "poster.jpg", "image/jpeg", imageBytes);

        ImageOptimizerService.OptimizedImage result = optimizerService.optimizeImage(file);

        assertNotNull(result);
        assertEquals("image/jpeg", result.contentType());
        assertTrue(result.size() > 0);
        assertEquals(800, result.width());
        assertEquals(600, result.height());

        // Verify decoded bytes
        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(result.bytes()));
        assertNotNull(decoded);
        assertEquals(800, decoded.getWidth());
        assertEquals(600, decoded.getHeight());
    }

    @Test
    @DisplayName("Should scale down large image to 1280 max width while preserving aspect ratio")
    void testOptimizeImage_WideImage_ScalesTo1280Width() throws IOException {
        // 2560x1440 (16:9 aspect ratio) -> should scale to 1280x720
        byte[] imageBytes = createTestImageBytes(2560, 1440, "png", false);
        MockMultipartFile file = new MockMultipartFile(
                "file", "wide_banner.png", "image/png", imageBytes);

        ImageOptimizerService.OptimizedImage result = optimizerService.optimizeImage(file);

        assertNotNull(result);
        assertEquals(1280, result.width());
        assertEquals(720, result.height());
        assertEquals("image/jpeg", result.contentType());

        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(result.bytes()));
        assertNotNull(decoded);
        assertEquals(1280, decoded.getWidth());
        assertEquals(720, decoded.getHeight());
    }

    @Test
    @DisplayName("Should scale down tall image to 1280 max height while preserving aspect ratio")
    void testOptimizeImage_TallImage_ScalesTo1280Height() throws IOException {
        // 1000x2000 (1:2 aspect ratio) -> should scale to 640x1280
        byte[] imageBytes = createTestImageBytes(1000, 2000, "jpg", false);
        MockMultipartFile file = new MockMultipartFile(
                "file", "tall_flyer.jpg", "image/jpeg", imageBytes);

        ImageOptimizerService.OptimizedImage result = optimizerService.optimizeImage(file);

        assertNotNull(result);
        assertEquals(640, result.width());
        assertEquals(1280, result.height());

        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(result.bytes()));
        assertNotNull(decoded);
        assertEquals(640, decoded.getWidth());
        assertEquals(1280, decoded.getHeight());
    }

    @Test
    @DisplayName("Should flatten transparent PNG to RGB white background cleanly without errors")
    void testOptimizeImage_TransparentPng_FlattensAlpha() throws IOException {
        byte[] imageBytes = createTestImageBytes(400, 400, "png", true);
        MockMultipartFile file = new MockMultipartFile(
                "file", "transparent_logo.png", "image/png", imageBytes);

        ImageOptimizerService.OptimizedImage result = optimizerService.optimizeImage(file);

        assertNotNull(result);
        assertEquals(400, result.width());
        assertEquals(400, result.height());
        assertEquals("image/jpeg", result.contentType());

        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(result.bytes()));
        assertNotNull(decoded);
        assertEquals(400, decoded.getWidth());
        assertEquals(400, decoded.getHeight());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when file is null or empty")
    void testValidateImage_EmptyFile_ThrowsException() {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", "empty.jpg", "image/jpeg", new byte[0]);

        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class, () ->
                optimizerService.validateImage(emptyFile));
        assertTrue(ex1.getMessage().contains("Cannot upload an empty or null file"));

        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class, () ->
                optimizerService.validateImage(null));
        assertTrue(ex2.getMessage().contains("Cannot upload an empty or null file"));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when file size exceeds 25MB")
    void testValidateImage_Exceeds25MB_ThrowsException() {
        MockMultipartFile largeFile = new MockMultipartFile(
                "file", "huge.jpg", "image/jpeg", new byte[10]) {
            @Override
            public long getSize() {
                return (26 * 1024 * 1024L); // 26MB
            }
        };

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                optimizerService.validateImage(largeFile));
        assertTrue(ex.getMessage().contains("File size exceeds maximum allowed limit of 25MB"));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when file type is not allowed")
    void testValidateImage_DisallowedContentType_ThrowsException() {
        MockMultipartFile textFile = new MockMultipartFile(
                "file", "script.sh", "text/plain", "echo hello".getBytes());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                optimizerService.validateImage(textFile));
        assertTrue(ex.getMessage().contains("Invalid file type"));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when extension is disallowed")
    void testValidateImage_DisallowedExtension_ThrowsException() {
        MockMultipartFile exeFile = new MockMultipartFile(
                "file", "malware.exe", "image/jpeg", new byte[]{1, 2, 3});

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                optimizerService.validateImage(exeFile));
        assertTrue(ex.getMessage().contains("Invalid file extension"));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when content is corrupted image")
    void testOptimizeImage_CorruptedBytes_ThrowsException() {
        MockMultipartFile corruptFile = new MockMultipartFile(
                "file", "corrupt.jpg", "image/jpeg", "this is definitely not a real image".getBytes());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                optimizerService.optimizeImage(corruptFile));
        assertTrue(ex.getMessage().contains("Unable to decode image"));
    }
}
