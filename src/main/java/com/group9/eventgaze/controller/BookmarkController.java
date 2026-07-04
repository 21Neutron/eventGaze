package com.group9.eventgaze.controller;

import com.group9.eventgaze.entity.dto.CustomUserDetails;
import com.group9.eventgaze.entity.dto.EventResponseDTO;
import com.group9.eventgaze.service.BookmarkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/eventgaze/bookmarks")
public class BookmarkController {

    @Autowired
    private BookmarkService bookmarkService;

    private Long extractUserId(CustomUserDetails userDetails) {
        if (userDetails != null && userDetails.getUserId() != null) {
            return userDetails.getUserId();
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            if (authentication.getPrincipal() instanceof CustomUserDetails cud) {
                return cud.getUserId();
            } else if (authentication.getName() != null) {
                try {
                    return Long.parseLong(authentication.getName());
                } catch (NumberFormatException ignored) {
                }
            }
        }
        throw new RuntimeException("Authenticated user ID not found");
    }

    @PostMapping("/{eventId}")
    public ResponseEntity<Void> addBookmark(
            @PathVariable Long eventId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = extractUserId(userDetails);
        bookmarkService.addBookmark(userId, eventId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{eventId}")
    public ResponseEntity<Void> removeBookmark(
            @PathVariable Long eventId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = extractUserId(userDetails);
        bookmarkService.removeBookmark(userId, eventId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/my-bookmarks")
    public ResponseEntity<List<EventResponseDTO>> getStudentBookmarks(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = extractUserId(userDetails);
        List<EventResponseDTO> bookmarks = bookmarkService.getStudentBookmarks(userId);
        return ResponseEntity.ok(bookmarks);
    }

    @GetMapping("/check/{eventId}")
    public ResponseEntity<Boolean> isEventBookmarked(
            @PathVariable Long eventId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = extractUserId(userDetails);
        boolean isBookmarked = bookmarkService.isEventBookmarked(userId, eventId);
        return ResponseEntity.ok(isBookmarked);
    }
}
