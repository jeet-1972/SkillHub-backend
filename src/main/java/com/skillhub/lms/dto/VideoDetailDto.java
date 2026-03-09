package com.skillhub.lms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoDetailDto {
    private Long id;
    private String title;
    private String description;
    private String youtubeUrl;
    private Integer orderIndex;
    private Integer durationSeconds;
    private Long sectionId;
    private String sectionTitle;
    private Long courseId;
    private String courseTitle;
    private Long previousVideoId;
    private Long nextVideoId;
    private Boolean locked;
    private String unlockReason;
}
