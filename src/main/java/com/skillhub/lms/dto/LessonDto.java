package com.skillhub.lms.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LessonDto {
    private Long id;
    private String title;
    private Integer sortOrder;
    private String youtubeUrl;
    private Integer durationMinutes;
}
