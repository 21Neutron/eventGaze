package com.group9.eventgaze.entity.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
public class EventRequestDTO {
    private String eventName;
    private String eventDescription;
    private String eventDate;
    private String eventTags;
    private Long eventCategoryId;
    private Long userId;
    private Long colleges;
    private MultipartFile eventArt;
}
