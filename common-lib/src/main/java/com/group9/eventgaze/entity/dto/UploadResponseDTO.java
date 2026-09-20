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
}
