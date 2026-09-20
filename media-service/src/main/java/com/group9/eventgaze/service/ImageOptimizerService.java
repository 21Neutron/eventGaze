package com.group9.eventgaze.service;

import net.coobird.thumbnailator.Thumbnails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Set;

@Service
public class ImageOptimizerService {

    private static final Logger log = LoggerFactory.getLogger(ImageOptimizerService.class);

    public static final long MAX_FILE_SIZE_BYTES = 25 * 1024 * 1024L; // 25 MB
    public static final int MAX_WIDTH = 1280;
    public static final int MAX_HEIGHT = 1280;
    public static final double COMPRESSION_QUALITY = 0.82; // 82% quality
    public static final String OUTPUT_FORMAT = "jpg";
    public static final String OUTPUT_CONTENT_TYPE = "image/jpeg";

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/webp",
            "image/gif"
    );

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "jpg",
            "jpeg",
            "png",
            "webp",
            "gif"
    );

    public record OptimizedImage(
            byte[] bytes,
            int width,
            int height,
            long size,
            String contentType
    ) {}

    /**
     * Validates that the uploaded file is present, within the 25MB limit, and has an allowed image type.
     *
     * @param file The multipart file to validate
     * @throws IllegalArgumentException If file validation fails
     */
    public void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Cannot upload an empty or null file.");
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("File size exceeds maximum allowed limit of 25MB. Current size: " +
                    (file.getSize() / (1024 * 1024)) + "MB.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !isAllowedContentType(contentType)) {
            throw new IllegalArgumentException("Invalid file type: '" + contentType +
                    "'. Allowed types: JPEG, PNG, WebP, GIF.");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null && originalFilename.contains(".")) {
            String extension = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();
            if (!ALLOWED_EXTENSIONS.contains(extension)) {
                throw new IllegalArgumentException("Invalid file extension: '." + extension +
                        "'. Allowed extensions: jpg, jpeg, png, webp, gif.");
            }
        }
    }

    /**
     * Optimizes an image: resizes to fit within 1280x1280 bounds maintaining aspect ratio,
     * and compresses to standardized JPEG with 82% quality.
     *
     * @param file The multipart image file
     * @return OptimizedImage containing compressed bytes and dimensions
     * @throws IOException If image reading or processing fails
     */
    public OptimizedImage optimizeImage(MultipartFile file) throws IOException {
        validateImage(file);

        BufferedImage inputImage;
        try {
            inputImage = ImageIO.read(file.getInputStream());
        } catch (Exception e) {
            log.error("Failed to read image stream: {}", e.getMessage(), e);
            throw new IllegalArgumentException("Unable to decode image file. Please upload a valid image.", e);
        }

        if (inputImage == null) {
            throw new IllegalArgumentException("Unable to decode image content: file is not a supported image format.");
        }

        return optimizeBufferedImage(inputImage);
    }

    /**
     * Internal optimization pipeline for a decoded BufferedImage.
     */
    public OptimizedImage optimizeBufferedImage(BufferedImage inputImage) throws IOException {
        int originalWidth = inputImage.getWidth();
        int originalHeight = inputImage.getHeight();

        // If the image has an alpha channel (transparency like PNG/GIF), draw onto white background for JPEG
        BufferedImage imageToProcess = inputImage;
        if (inputImage.getColorModel().hasAlpha()) {
            BufferedImage rgbImage = new BufferedImage(
                    originalWidth,
                    originalHeight,
                    BufferedImage.TYPE_INT_RGB);
            Graphics2D g = rgbImage.createGraphics();
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, originalWidth, originalHeight);
            g.drawImage(inputImage, 0, 0, null);
            g.dispose();
            imageToProcess = rgbImage;
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        var thumbnailBuilder = Thumbnails.of(imageToProcess);

        // Scale down to max bounds (1280x1280) preserving aspect ratio.
        // If already within bounds, preserve original dimensions without upscaling.
        if (originalWidth > MAX_WIDTH || originalHeight > MAX_HEIGHT) {
            thumbnailBuilder.size(MAX_WIDTH, MAX_HEIGHT);
        } else {
            thumbnailBuilder.size(originalWidth, originalHeight);
        }

        thumbnailBuilder.outputFormat(OUTPUT_FORMAT)
                .outputQuality(COMPRESSION_QUALITY)
                .toOutputStream(outputStream);

        byte[] compressedBytes = outputStream.toByteArray();

        // Calculate resulting dimensions
        double scale = 1.0;
        if (originalWidth > MAX_WIDTH || originalHeight > MAX_HEIGHT) {
            scale = Math.min((double) MAX_WIDTH / originalWidth, (double) MAX_HEIGHT / originalHeight);
        }
        int finalWidth = Math.max(1, (int) Math.round(originalWidth * scale));
        int finalHeight = Math.max(1, (int) Math.round(originalHeight * scale));

        log.info("Optimized image: [{}x{}] -> [{}x{}], compressed size: {} bytes (format: JPEG, quality: 82%)",
                originalWidth, originalHeight, finalWidth, finalHeight, compressedBytes.length);

        return new OptimizedImage(
                compressedBytes,
                finalWidth,
                finalHeight,
                compressedBytes.length,
                OUTPUT_CONTENT_TYPE
        );
    }

    private boolean isAllowedContentType(String contentType) {
        String cleanType = contentType.split(";")[0].trim().toLowerCase();
        return ALLOWED_CONTENT_TYPES.contains(cleanType);
    }
}
