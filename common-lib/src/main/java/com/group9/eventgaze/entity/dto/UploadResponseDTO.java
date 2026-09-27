package com.group9.eventgaze.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadResponseDTO {
    private String fileUrl;
    private String key;
    private Long fileSize;
    private String contentType;

    private String thumbnailKey;
    private String thumbnailUrl;
    private Integer width;
    private Integer height;
    private Integer thumbWidth;
    private Integer thumbHeight;
    private Long thumbFileSize;
}
