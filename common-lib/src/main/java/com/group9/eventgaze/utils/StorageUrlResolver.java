package com.group9.eventgaze.utils;

import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * Utility and service component for resolving relative storage keys to complete public URLs
 * and extracting relative object keys from S3/CDN URLs.
 */
@Component
public class StorageUrlResolver {

    /**
     * Resolves a key or URL against a base URL.
     * - If keyOrUrl is null or blank, returns null.
     * - If keyOrUrl is already an absolute URL (e.g. http://, https://, //, or placeholder like https://picsum...),
     *   it is returned as is for smooth backward compatibility.
     * - If keyOrUrl is a relative key (e.g. "events/123.jpg"), baseUrl is prepended, guaranteeing no double slashes.
     * - If baseUrl is null or blank, returns the relative key (leading slashes removed).
     *
     * @param keyOrUrl Relative key or absolute URL
     * @param baseUrl  Storage or CDN base URL (e.g. "https://eventgazeeventimages.s3.amazonaws.com/")
     * @return Fully resolved public URL or null
     */
    public String resolveUrl(String keyOrUrl, String baseUrl) {
        return resolve(keyOrUrl, baseUrl);
    }

    /**
     * Static helper to resolve a key or URL against a base URL.
     *
     * @param keyOrUrl Relative key or absolute URL
     * @param baseUrl  Storage or CDN base URL
     * @return Fully resolved public URL or null
     */
    public static String resolve(String keyOrUrl, String baseUrl) {
        if (keyOrUrl == null || keyOrUrl.isBlank()) {
            return null;
        }

        String trimmedKey = keyOrUrl.trim();

        // If input is already an absolute URL (http, https), return as is for backward compatibility
        if (trimmedKey.startsWith("http://") || trimmedKey.startsWith("https://")) {
            return trimmedKey;
        }

        while (trimmedKey.startsWith("/")) {
            trimmedKey = trimmedKey.substring(1);
        }

        if (baseUrl == null || baseUrl.isBlank()) {
            return trimmedKey;
        }

        String trimmedBase = baseUrl.trim();
        while (trimmedBase.endsWith("/")) {
            trimmedBase = trimmedBase.substring(0, trimmedBase.length() - 1);
        }

        return trimmedBase + "/" + trimmedKey;
    }

    /**
     * Extracts a relative object key from a full URL or returns the sanitized relative key.
     * Preserves external absolute placeholder URLs (e.g. picsum.photos, dummyimage).
     *
     * @param keyOrUrl   Raw key or S3/CDN URL
     * @param bucketName Optional bucket name to strip if present in path-style URLs
     * @return Cleaned relative key, or placeholder URL, or null
     */
    public static String toRelativeKey(String keyOrUrl, String bucketName) {
        if (keyOrUrl == null || keyOrUrl.isBlank()) {
            return null;
        }

        String trimmed = keyOrUrl.trim();

        // External placeholders should remain intact
        if (isExternalPlaceholder(trimmed)) {
            return trimmed;
        }

        // If not an HTTP(S) URL, treat directly as relative key
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            return cleanKey(trimmed, bucketName);
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

            return cleanKey(path, bucketName);
        } catch (Exception e) {
            return cleanKey(extractKeyFallback(trimmed, bucketName), bucketName);
        }
    }

    /**
     * Extracts relative key with no bucket name specification.
     *
     * @param keyOrUrl Raw key or S3/CDN URL
     * @return Cleaned relative key or null
     */
    public static String toRelativeKey(String keyOrUrl) {
        return toRelativeKey(keyOrUrl, null);
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

    private static String cleanKey(String key, String bucketName) {
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
        if (bucketName != null && !bucketName.isBlank() && key.startsWith(bucketName + "/")) {
            key = key.substring(bucketName.length() + 1);
        }
        while (key.startsWith("/")) {
            key = key.substring(1);
        }
        return key.isBlank() ? null : key;
    }

    /**
     * Checks if a URL or key is an external placeholder service URL.
     */
    public static boolean isExternalPlaceholder(String urlOrKey) {
        if (urlOrKey == null || urlOrKey.isBlank()) {
            return false;
        }
        String lower = urlOrKey.trim().toLowerCase();
        if (lower.startsWith("http://") || lower.startsWith("https://")) {
            return lower.contains("picsum.photos") ||
                    lower.contains("dummyimage") ||
                    lower.contains("placeholder.com") ||
                    lower.contains("via.placeholder");
        }
        return false;
    }

    /**
     * Checks if a URL or key represents a placeholder or default image.
     */
    public static boolean isPlaceholder(String urlOrKey) {
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
}
