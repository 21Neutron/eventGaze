package com.group9.eventgaze.service;

import com.group9.eventgaze.entity.dto.UploadResponseDTO;
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

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
public class MediaService {

    private static final Logger log = LoggerFactory.getLogger(MediaService.class);

    private final ImageOptimizerService imageOptimizerService;
    private final S3Client s3Client;

    @Value("${AWS.S3.BUCKET_NAME:${aws.s3.bucket-name:eventgazeeventimages}}")
    private String bucketName = "eventgazeeventimages";

    @Autowired
    public MediaService(ImageOptimizerService imageOptimizerService, S3Client s3Client) {
        this.imageOptimizerService = imageOptimizerService;
        this.s3Client = s3Client;
    }

    public MediaService(ImageOptimizerService imageOptimizerService, S3Client s3Client, String bucketName) {
        this.imageOptimizerService = imageOptimizerService;
        this.s3Client = s3Client;
        this.bucketName = bucketName;
    }

    /**
     * Optimizes the uploaded image and streams the compressed bytes directly to AWS S3.
     * Generates a collision-proof UUID key: <folder>/<uuid>.jpg.
     *
     * @param file The multipart image file
     * @param folder Directory folder prefix (e.g. "events")
     * @return UploadResponseDTO containing fileUrl, key, fileSize, and contentType
     * @throws IOException If image optimization or S3 streaming fails
     */
    public UploadResponseDTO uploadMedia(MultipartFile file, String folder) throws IOException {
        String key = generateKey(folder);

        ImageOptimizerService.OptimizedImage optimized = imageOptimizerService.optimizeImage(file);

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(optimized.contentType())
                    .contentLength(optimized.size())
                    .build();

            log.info("Streaming optimized image to S3: bucket='{}', key='{}', size={} bytes",
                    bucketName, key, optimized.size());

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(optimized.bytes()));

            String fileUrl = s3Client.utilities().getUrl(b -> b.bucket(bucketName).key(key)).toString();
            log.info("Media successfully uploaded to S3: {}", fileUrl);

            return UploadResponseDTO.builder()
                    .fileUrl(fileUrl)
                    .key(key)
                    .fileSize(optimized.size())
                    .contentType(optimized.contentType())
                    .build();

        } catch (S3Exception e) {
            log.error("AWS S3 service error while uploading key '{}' to bucket '{}': {}",
                    key, bucketName, e.awsErrorDetails() != null ? e.awsErrorDetails().errorMessage() : e.getMessage(), e);
            throw new IOException("Failed to upload media to S3: " +
                    (e.awsErrorDetails() != null ? e.awsErrorDetails().errorMessage() : e.getMessage()), e);
        } catch (SdkException e) {
            log.error("AWS SDK error while uploading key '{}' to bucket '{}': {}",
                    key, bucketName, e.getMessage(), e);
            throw new IOException("Failed to communicate with AWS S3: " + e.getMessage(), e);
        }
    }

    /**
     * Deletes media asset from S3 by key or URL.
     *
     * @param key S3 object key (optional if url is provided)
     * @param url S3 object URL (optional if key is provided)
     * @throws IllegalArgumentException If neither key nor url is provided
     */
    public void deleteMedia(String key, String url) {
        if ((key != null && isDefaultPlaceholder(key)) || (url != null && isDefaultPlaceholder(url))) {
            log.info("Skipping deletion of placeholder asset: key='{}', url='{}'", key, url);
            return;
        }

        String targetKey = null;
        if (key != null && !key.isBlank()) {
            targetKey = cleanKey(key);
        } else if (url != null && !url.isBlank()) {
            targetKey = extractKeyFromUrl(url);
        } else {
            throw new IllegalArgumentException("Either 'key' or 'url' must be provided for deletion.");
        }

        if (targetKey == null || targetKey.isBlank() || isDefaultPlaceholder(targetKey)) {
            log.info("Skipping deletion of S3 asset (null, empty, or default placeholder): '{}'", targetKey);
            return;
        }

        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(targetKey)
                    .build();

            s3Client.deleteObject(deleteRequest);
            log.info("Successfully deleted media from S3: bucket='{}', key='{}'", bucketName, targetKey);
        } catch (Exception e) {
            log.error("Failed to delete media from S3: bucket='{}', key='{}': {}",
                    bucketName, targetKey, e.getMessage(), e);
        }
    }

    /**
     * Generates a collision-proof UUID key: <folder>/<uuid>.jpg.
     */
    public String generateKey(String folder) {
        String cleanFolder = (folder != null && !folder.isBlank()) ? folder.trim() : "events";
        cleanFolder = cleanFolder.replace('\\', '/');
        cleanFolder = cleanFolder.replaceAll("^/+", "").replaceAll("/+$", "");
        if (cleanFolder.isBlank()) {
            cleanFolder = "events";
        }
        return cleanFolder + "/" + UUID.randomUUID() + ".jpg";
    }

    /**
     * Extracts S3 object key from a full URL or returns cleaned key.
     */
    public String extractKeyFromUrl(String url) {
        return extractKeyFromUrl(url, this.bucketName);
    }

    public static String extractKeyFromUrl(String url, String bucketName) {
        if (url == null || url.isBlank()) {
            return null;
        }

        String trimmed = url.trim();

        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            return cleanKey(trimmed);
        }

        try {
            URI uri = URI.create(trimmed);
            String path = uri.getPath();
            if (path == null || path.isBlank()) {
                return null;
            }

            while (path.startsWith("/")) {
                path = path.substring(1);
            }

            String host = uri.getHost() != null ? uri.getHost().toLowerCase() : "";

            if (bucketName != null && !bucketName.isBlank() && path.startsWith(bucketName + "/")) {
                path = path.substring(bucketName.length() + 1);
            } else if (host.startsWith("s3.") || host.startsWith("s3-") || host.equals("s3.amazonaws.com")) {
                int firstSlash = path.indexOf('/');
                if (firstSlash != -1 && firstSlash < path.length() - 1) {
                    path = path.substring(firstSlash + 1);
                }
            }

            return cleanKey(path);
        } catch (Exception e) {
            log.warn("Failed to parse S3 key using URI for '{}', falling back: {}", url, e.getMessage());
            return cleanKey(extractKeyFallback(trimmed, bucketName));
        }
    }

    private static String extractKeyFallback(String url, String bucketName) {
        try {
            String withoutScheme = url.replaceFirst("^https?://", "");
            int queryIndex = withoutScheme.indexOf('?');
            if (queryIndex != -1) {
                withoutScheme = withoutScheme.substring(0, queryIndex);
            }
            int slashIndex = withoutScheme.indexOf('/');
            if (slashIndex == -1 || slashIndex == withoutScheme.length() - 1) {
                return null;
            }
            String path = withoutScheme.substring(slashIndex + 1);
            if (bucketName != null && !bucketName.isBlank() && path.startsWith(bucketName + "/")) {
                path = path.substring(bucketName.length() + 1);
            }
            return path;
        } catch (Exception e) {
            return null;
        }
    }

    private static String cleanKey(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        while (key.startsWith("/")) {
            key = key.substring(1);
        }
        try {
            key = URLDecoder.decode(key, StandardCharsets.UTF_8);
        } catch (Exception ignored) {
        }
        return key.isBlank() ? null : key;
    }

    public boolean isDefaultPlaceholder(String urlOrKey) {
        if (urlOrKey == null || urlOrKey.isBlank()) {
            return false;
        }
        String lower = urlOrKey.trim().toLowerCase();

        if (lower.contains("placeholder") || lower.contains("dummyimage") || lower.contains("picsum.photos")) {
            return true;
        }

        if (lower.contains("default")) {
            return true;
        }

        return lower.startsWith("/static/") || lower.startsWith("/assets/") || lower.startsWith("/images/");
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }
}
