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
     * Optimizes the uploaded image and streams both full-resolution and thumbnail compressed bytes to AWS S3.
     * Generates collision-proof UUID keys: <folder>/<uuid>.jpg and <folder>/<uuid>-thumb.jpg.
     *
     * @param file The multipart image file
     * @param folder Directory folder prefix (e.g. "events")
     * @return UploadResponseDTO containing fileUrl, key, fileSize, contentType, and thumbnail details
     * @throws IOException If image optimization or S3 streaming fails
     */
    public UploadResponseDTO uploadMedia(MultipartFile file, String folder) throws IOException {
        String mainKey = generateKey(folder);
        String thumbKey = toThumbnailKey(mainKey);

        ImageOptimizerService.MultiVariantImage multiVariant = imageOptimizerService.optimizeImageWithThumbnail(file);
        ImageOptimizerService.OptimizedImage fullImage = multiVariant.fullImage();
        ImageOptimizerService.OptimizedImage thumbnail = multiVariant.thumbnail();

        try {
            // Upload full image
            PutObjectRequest putFullRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(mainKey)
                    .contentType(fullImage.contentType())
                    .contentLength(fullImage.size())
                    .build();

            log.info("Streaming full optimized image to S3: bucket='{}', key='{}', size={} bytes",
                    bucketName, mainKey, fullImage.size());
            s3Client.putObject(putFullRequest, RequestBody.fromBytes(fullImage.bytes()));
            String fileUrl = s3Client.utilities().getUrl(b -> b.bucket(bucketName).key(mainKey)).toString();

            // Upload thumbnail
            PutObjectRequest putThumbRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(thumbKey)
                    .contentType(thumbnail.contentType())
                    .contentLength(thumbnail.size())
                    .build();

            log.info("Streaming thumbnail image to S3: bucket='{}', key='{}', size={} bytes",
                    bucketName, thumbKey, thumbnail.size());
            s3Client.putObject(putThumbRequest, RequestBody.fromBytes(thumbnail.bytes()));
            String thumbnailUrl = s3Client.utilities().getUrl(b -> b.bucket(bucketName).key(thumbKey)).toString();

            log.info("Media successfully uploaded to S3: full='{}', thumb='{}'", fileUrl, thumbnailUrl);

            return UploadResponseDTO.builder()
                    .fileUrl(fileUrl)
                    .key(mainKey)
                    .fileSize(fullImage.size())
                    .contentType(fullImage.contentType())
                    .width(fullImage.width())
                    .height(fullImage.height())
                    .thumbnailKey(thumbKey)
                    .thumbnailUrl(thumbnailUrl)
                    .thumbWidth(thumbnail.width())
                    .thumbHeight(thumbnail.height())
                    .thumbFileSize(thumbnail.size())
                    .build();

        } catch (S3Exception e) {
            log.error("AWS S3 service error while uploading to bucket '{}': {}",
                    bucketName, e.awsErrorDetails() != null ? e.awsErrorDetails().errorMessage() : e.getMessage(), e);
            throw new IOException("Failed to upload media to S3: " +
                    (e.awsErrorDetails() != null ? e.awsErrorDetails().errorMessage() : e.getMessage()), e);
        } catch (SdkException e) {
            log.error("AWS SDK error while uploading to bucket '{}': {}",
                    bucketName, e.getMessage(), e);
            throw new IOException("Failed to communicate with AWS S3: " + e.getMessage(), e);
        }
    }

    public UploadResponseDTO uploadMedia(String folder, MultipartFile file) throws IOException {
        return uploadMedia(file, folder);
    }

    /**
     * Deletes media asset from S3 by key or URL. If deleting a main image or thumbnail,
     * also cleans up the associated variant from S3.
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

        // Delete primary target key
        deleteSingleKey(targetKey);

        // Also clean up associated companion variant (main <-> thumbnail)
        String companionKey = targetKey.contains("-thumb.") ? toMainKey(targetKey) : toThumbnailKey(targetKey);
        if (companionKey != null && !companionKey.equals(targetKey) && !isDefaultPlaceholder(companionKey)) {
            deleteSingleKey(companionKey);
        }
    }

    private void deleteSingleKey(String s3Key) {
        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();

            s3Client.deleteObject(deleteRequest);
            log.info("Successfully deleted media from S3: bucket='{}', key='{}'", bucketName, s3Key);
        } catch (Exception e) {
            log.error("Failed to delete media from S3: bucket='{}', key='{}': {}",
                    bucketName, s3Key, e.getMessage(), e);
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
     * Converts a main object key into its associated thumbnail key.
     * e.g., "events/uuid.jpg" -> "events/uuid-thumb.jpg".
     */
    public static String toThumbnailKey(String mainKey) {
        if (mainKey == null || mainKey.isBlank()) {
            return null;
        }
        if (mainKey.endsWith("-thumb.jpg")) {
            return mainKey;
        }
        if (mainKey.endsWith(".jpg")) {
            return mainKey.substring(0, mainKey.length() - 4) + "-thumb.jpg";
        }
        int dotIndex = mainKey.lastIndexOf('.');
        if (dotIndex != -1) {
            return mainKey.substring(0, dotIndex) + "-thumb" + mainKey.substring(dotIndex);
        }
        return mainKey + "-thumb.jpg";
    }

    /**
     * Converts a thumbnail key back to the main object key.
     * e.g., "events/uuid-thumb.jpg" -> "events/uuid.jpg".
     */
    public static String toMainKey(String thumbKey) {
        if (thumbKey == null || thumbKey.isBlank()) {
            return null;
        }
        if (thumbKey.contains("-thumb.")) {
            return thumbKey.replace("-thumb.", ".");
        }
        if (thumbKey.endsWith("-thumb")) {
            return thumbKey.substring(0, thumbKey.length() - "-thumb".length()) + ".jpg";
        }
        return thumbKey;
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
