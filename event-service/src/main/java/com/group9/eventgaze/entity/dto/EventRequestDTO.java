package com.group9.eventgaze.entity.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventRequestDTO {
    private String eventName;
    private String eventDescription;
    private String eventDate;
    private String eventTags;
    private Long eventCategoryId;
    private Long userId;
    private Long colleges;

    @JsonIgnore
    private MultipartFile eventArt;

    private String eventArtKey;
    private String eventArtUrl;

    @JsonProperty("eventArt")
    public void setEventArtFromJson(String eventArt) {
        if (eventArt != null && !eventArt.isBlank()) {
            this.eventArtUrl = eventArt.trim();
        }
    }
}
