package com.group9.eventgaze.controller;

import com.group9.eventgaze.entity.dto.UploadResponseDTO;
import com.group9.eventgaze.service.MediaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/eventgaze/media")
public class MediaController {

    private final MediaService mediaService;

    @Autowired
    public MediaController(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    /**
     * Uploads and optimizes a multipart image file and streams it directly to AWS S3.
     *
     * @param file The multipart image file to upload and optimize
     * @param folder The folder path prefix (default: "events")
     * @return UploadResponseDTO with fileUrl, key, fileSize, and contentType
     * @throws IOException If streaming or optimization fails
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UploadResponseDTO> uploadMedia(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", defaultValue = "events") String folder) throws IOException {

        UploadResponseDTO response = mediaService.uploadMedia(file, folder);
        return ResponseEntity.ok(response);
    }

    /**
     * Deletes a media asset from AWS S3 using either key or URL via /delete.
     *
     * @param key S3 object key
     * @param url S3 object URL
     * @return 204 No Content
     */
    @DeleteMapping("/delete")
    public ResponseEntity<Void> deleteMedia(
            @RequestParam(value = "key", required = false) String key,
            @RequestParam(value = "url", required = false) String url) {

        mediaService.deleteMedia(key, url);
        return ResponseEntity.noContent().build();
    }

    /**
     * Deletes a media asset from AWS S3 using either key or URL via root endpoint.
     *
     * @param key S3 object key
     * @param url S3 object URL
     * @return 204 No Content
     */
    @DeleteMapping
    public ResponseEntity<Void> deleteMediaRoot(
            @RequestParam(value = "key", required = false) String key,
            @RequestParam(value = "url", required = false) String url) {

        mediaService.deleteMedia(key, url);
        return ResponseEntity.noContent().build();
    }
}
