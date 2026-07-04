package com.group9.eventgaze.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.UUID;

@Service
public class S3Service {

    private static final Logger log = LoggerFactory.getLogger(S3Service.class);

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

    @Autowired
    private S3Client s3client;

    @Value("${AWS.S3.BUCKET_NAME:eventgazeeventimages}")
    private String bucketName = "eventgazeeventimages";

    public S3Service() {
    }

    public S3Service(S3Client s3client) {
        this.s3client = s3client;
    }

    public S3Service(S3Client s3client, String bucketName) {
        this.s3client = s3client;
        this.bucketName = bucketName;
    }

    /**
     * Uploads an image file to AWS S3 directly from the MultipartFile stream without buffering to local disk.
     * Generates a unique key and validates file content type.
     *
     * @param folderOrKey Folder prefix (e.g. "events") or existing key path
     * @param file The multipart image file to upload
     * @return The public URL of the uploaded S3 object
     * @throws IOException If streaming fails or S3 communication fails
     * @throws IllegalArgumentException If the file is empty or not an allowed image format
     */
    public String uploadFile(String folderOrKey, MultipartFile file) throws IOException {
        validateImageFile(file);

        String uniqueKey = generateUniqueKey(folderOrKey, file);

        try (InputStream inputStream = file.getInputStream()) {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(uniqueKey)
                    .contentType(file.getContentType())
                    .build();

            log.info("Uploading file to S3: bucket={}, key={}, size={} bytes, contentType={}",
                    bucketName, uniqueKey, file.getSize(), file.getContentType());

            s3client.putObject(putObjectRequest, RequestBody.fromInputStream(inputStream, file.getSize()));

            String fileUrl = s3client.utilities().getUrl(b -> b.bucket(bucketName).key(uniqueKey)).toString();
            log.info("File uploaded successfully to S3: {}", fileUrl);
            return fileUrl;
        } catch (S3Exception e) {
            log.error("AWS S3 service error while uploading key '{}' to bucket '{}': {}",
                    uniqueKey, bucketName, e.awsErrorDetails() != null ? e.awsErrorDetails().errorMessage() : e.getMessage(), e);
            throw new IOException("Failed to upload file to S3: " +
                    (e.awsErrorDetails() != null ? e.awsErrorDetails().errorMessage() : e.getMessage()), e);
        } catch (SdkException e) {
            log.error("AWS SDK error while uploading key '{}' to bucket '{}': {}",
                    uniqueKey, bucketName, e.getMessage(), e);
            throw new IOException("Failed to communicate with AWS S3: " + e.getMessage(), e);
        }
    }

    /**
     * Uploads an image file to AWS S3 using the default "events" folder.
     *
     * @param file The multipart image file to upload
     * @return The public URL of the uploaded S3 object
     * @throws IOException If streaming fails or S3 communication fails
     * @throws IllegalArgumentException If the file is empty or not an allowed image format
     */
    public String uploadFile(MultipartFile file) throws IOException {
        return uploadFile("events", file);
    }

    /**
     * Deletes an object from S3 by its key.
     *
     * @param key The key of the object to delete
     */
    public void deleteFile(String key) {
        if (key == null || key.isBlank()) {
            return;
        }
        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();
            s3client.deleteObject(deleteObjectRequest);
            log.info("Successfully deleted file from S3: bucket={}, key={}", bucketName, key);
        } catch (Exception e) {
            log.error("Failed to delete file from S3: bucket={}, key={}", bucketName, key, e);
        }
    }

    /**
     * Validates that the uploaded file is not null/empty and has an allowed image MIME type and extension.
     *
     * @param file The multipart file to validate
     * @throws IllegalArgumentException If the file is invalid
     */
    public void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Cannot upload an empty or null file.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !isAllowedContentType(contentType)) {
            throw new IllegalArgumentException("Invalid file type: '" + contentType + "'. Allowed types: JPEG, PNG, WebP, GIF.");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null && originalFilename.contains(".")) {
            String extension = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();
            if (!ALLOWED_EXTENSIONS.contains(extension)) {
                throw new IllegalArgumentException("Invalid file extension: '." + extension + "'. Allowed extensions: jpg, jpeg, png, webp, gif.");
            }
        }
    }

    private boolean isAllowedContentType(String contentType) {
        String cleanType = contentType.split(";")[0].trim().toLowerCase();
        return ALLOWED_CONTENT_TYPES.contains(cleanType);
    }

    /**
     * Generates a unique S3 object key combining a folder prefix, a random UUID, and the sanitized original filename.
     *
     * @param folderOrKey Directory prefix (e.g. "events") or existing path
     * @param file The multipart file
     * @return A unique S3 object key
     */
    public String generateUniqueKey(String folderOrKey, MultipartFile file) {
        String originalFilename = file != null ? file.getOriginalFilename() : null;
        String sanitizedName = sanitizeFilename(originalFilename);
        String uniqueFileName = UUID.randomUUID().toString() + "-" + sanitizedName;

        if (folderOrKey == null || folderOrKey.isBlank()) {
            return uniqueFileName;
        }

        String prefix = folderOrKey.replace('\\', '/').trim();

        // If folderOrKey ends with the original filename, strip it so we only keep the directory
        if (originalFilename != null && !originalFilename.isBlank() && prefix.endsWith(originalFilename)) {
            prefix = prefix.substring(0, prefix.length() - originalFilename.length());
        } else if (prefix.contains("/") && prefix.substring(prefix.lastIndexOf('/') + 1).contains(".")) {
            prefix = prefix.substring(0, prefix.lastIndexOf('/'));
        }

        // Clean leading and trailing slashes
        while (prefix.startsWith("/")) {
            prefix = prefix.substring(1);
        }
        while (prefix.endsWith("/")) {
            prefix = prefix.substring(0, prefix.length() - 1);
        }

        if (prefix.isBlank()) {
            return uniqueFileName;
        }

        return prefix + "/" + uniqueFileName;
    }

    /**
     * Sanitizes the original filename to prevent path traversal and remove unsafe characters.
     *
     * @param originalFilename Raw original filename from client
     * @return Sanitized filename
     */
    public String sanitizeFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "image.jpg";
        }
        // Extract base name only (strip any directory traversal)
        String baseName = new File(originalFilename).getName();
        // Replace non-alphanumeric (except dot, dash, underscore) with underscore
        String sanitized = baseName.replaceAll("[^a-zA-Z0-9._-]", "_");
        // Strip leading dots or hyphens
        sanitized = sanitized.replaceAll("^[.-]+", "");
        return sanitized.isBlank() ? "image.jpg" : sanitized;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }
}
