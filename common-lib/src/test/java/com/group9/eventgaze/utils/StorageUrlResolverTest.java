package com.group9.eventgaze.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class StorageUrlResolverTest {

    private StorageUrlResolver resolver;
    private static final String DEFAULT_BASE_URL = "https://eventgazeeventimages.s3.amazonaws.com/";

    @BeforeEach
    void setUp() {
        resolver = new StorageUrlResolver();
    }

    @Test
    @DisplayName("resolveUrl should prepend baseUrl to relative key without double slashes")
    void testResolveUrl_relativeKey() {
        String result = resolver.resolveUrl("events/uuid-123.jpg", DEFAULT_BASE_URL);
        assertEquals("https://eventgazeeventimages.s3.amazonaws.com/events/uuid-123.jpg", result);
    }

    @Test
    @DisplayName("resolveUrl should handle relative key with leading slash and baseUrl without trailing slash")
    void testResolveUrl_relativeKeyWithLeadingSlash() {
        String baseUrlWithoutSlash = "https://cdn.eventgaze.com";
        String result = resolver.resolveUrl("/events/uuid-123.jpg", baseUrlWithoutSlash);
        assertEquals("https://cdn.eventgaze.com/events/uuid-123.jpg", result);
    }

    @Test
    @DisplayName("resolveUrl should normalize multiple slashes between base and key")
    void testResolveUrl_multipleSlashesNormalized() {
        String baseUrlWithMultipleSlashes = "https://cdn.eventgaze.com///";
        String result = resolver.resolveUrl("///events/uuid-123.jpg", baseUrlWithMultipleSlashes);
        assertEquals("https://cdn.eventgaze.com/events/uuid-123.jpg", result);
    }

    @Test
    @DisplayName("resolveUrl should return absolute URLs unchanged for backward compatibility")
    void testResolveUrl_absoluteUrlsUnchanged() {
        String picsumUrl = "https://picsum.photos/200/300";
        assertEquals(picsumUrl, resolver.resolveUrl(picsumUrl, DEFAULT_BASE_URL));

        String httpUrl = "http://example.com/images/poster.jpg";
        assertEquals(httpUrl, resolver.resolveUrl(httpUrl, DEFAULT_BASE_URL));

        String legacyS3Url = "https://eventgazeeventimages.s3.amazonaws.com/events/old.png";
        assertEquals(legacyS3Url, resolver.resolveUrl(legacyS3Url, DEFAULT_BASE_URL));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    @DisplayName("resolveUrl should return null for null or blank input")
    void testResolveUrl_nullOrBlankReturnsNull(String input) {
        assertNull(resolver.resolveUrl(input, DEFAULT_BASE_URL));
        assertNull(StorageUrlResolver.resolve(input, DEFAULT_BASE_URL));
    }

    @Test
    @DisplayName("resolveUrl should return sanitized relative key when baseUrl is null or blank")
    void testResolveUrl_nullOrBlankBaseUrl() {
        assertEquals("events/123.jpg", resolver.resolveUrl("events/123.jpg", null));
        assertEquals("events/123.jpg", resolver.resolveUrl("/events/123.jpg", ""));
        assertEquals("events/123.jpg", resolver.resolveUrl("events/123.jpg", "   "));
    }

    @Test
    @DisplayName("toRelativeKey should preserve relative keys and strip leading slashes")
    void testToRelativeKey_relativeKey() {
        assertEquals("events/uuid-123.jpg", StorageUrlResolver.toRelativeKey("events/uuid-123.jpg", "eventgazeeventimages"));
        assertEquals("events/uuid-123.jpg", StorageUrlResolver.toRelativeKey("/events/uuid-123.jpg", "eventgazeeventimages"));
        assertEquals("events/uuid-123.jpg", StorageUrlResolver.toRelativeKey("events/uuid-123.jpg"));
    }

    @Test
    @DisplayName("toRelativeKey should extract key from virtual-hosted S3 URL")
    void testToRelativeKey_virtualHostedS3Url() {
        String url = "https://eventgazeeventimages.s3.amazonaws.com/events/uuid-123.jpg";
        assertEquals("events/uuid-123.jpg", StorageUrlResolver.toRelativeKey(url, "eventgazeeventimages"));

        String regionalUrl = "https://eventgazeeventimages.s3.us-west-2.amazonaws.com/events/sub/uuid-123.jpg";
        assertEquals("events/sub/uuid-123.jpg", StorageUrlResolver.toRelativeKey(regionalUrl, "eventgazeeventimages"));
    }

    @Test
    @DisplayName("toRelativeKey should extract key from path-style S3 URL")
    void testToRelativeKey_pathStyleS3Url() {
        String pathStyleUrl = "https://s3.amazonaws.com/eventgazeeventimages/events/uuid-123.jpg";
        assertEquals("events/uuid-123.jpg", StorageUrlResolver.toRelativeKey(pathStyleUrl, "eventgazeeventimages"));

        String regionalPathStyle = "https://s3.eu-central-1.amazonaws.com/eventgazeeventimages/events/uuid-123.jpg";
        assertEquals("events/uuid-123.jpg", StorageUrlResolver.toRelativeKey(regionalPathStyle, "eventgazeeventimages"));
    }

    @Test
    @DisplayName("toRelativeKey should extract key from CDN URL")
    void testToRelativeKey_cdnUrl() {
        String cdnUrl = "https://cdn.eventgaze.com/events/uuid-123.jpg";
        assertEquals("events/uuid-123.jpg", StorageUrlResolver.toRelativeKey(cdnUrl, "eventgazeeventimages"));
    }

    @Test
    @DisplayName("toRelativeKey should preserve external placeholder URLs")
    void testToRelativeKey_preservesPlaceholders() {
        String picsumUrl = "https://picsum.photos/200/300";
        assertEquals(picsumUrl, StorageUrlResolver.toRelativeKey(picsumUrl, "eventgazeeventimages"));

        String dummyImageUrl = "https://dummyimage.com/600x400/000/fff";
        assertEquals(dummyImageUrl, StorageUrlResolver.toRelativeKey(dummyImageUrl, "eventgazeeventimages"));

        String viaPlaceholder = "https://via.placeholder.com/300";
        assertEquals(viaPlaceholder, StorageUrlResolver.toRelativeKey(viaPlaceholder, "eventgazeeventimages"));
    }

    @Test
    @DisplayName("toRelativeKey should decode URL-encoded keys and strip query params")
    void testToRelativeKey_urlDecodedAndQueryParams() {
        String encodedUrl = "https://eventgazeeventimages.s3.amazonaws.com/events/my%20event%20poster.jpg?AWSAccessKeyId=test";
        assertEquals("events/my event poster.jpg", StorageUrlResolver.toRelativeKey(encodedUrl, "eventgazeeventimages"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    @DisplayName("toRelativeKey should return null for null or blank input")
    void testToRelativeKey_nullOrBlank(String input) {
        assertNull(StorageUrlResolver.toRelativeKey(input, "eventgazeeventimages"));
        assertNull(StorageUrlResolver.toRelativeKey(input));
    }

    @Test
    @DisplayName("isPlaceholder should identify default and placeholder patterns")
    void testIsPlaceholder() {
        assertTrue(StorageUrlResolver.isPlaceholder("https://picsum.photos/200/300"));
        assertTrue(StorageUrlResolver.isPlaceholder("events/default-poster.png"));
        assertTrue(StorageUrlResolver.isPlaceholder("placeholder.jpg"));
        assertTrue(StorageUrlResolver.isPlaceholder("/assets/default.png"));
        assertTrue(StorageUrlResolver.isPlaceholder("/static/placeholder.jpg"));

        assertFalse(StorageUrlResolver.isPlaceholder("events/real-flyer-uuid.jpg"));
        assertFalse(StorageUrlResolver.isPlaceholder("https://eventgazeeventimages.s3.amazonaws.com/events/flyer.png"));
        assertFalse(StorageUrlResolver.isPlaceholder(null));
        assertFalse(StorageUrlResolver.isPlaceholder(""));
    }
}
