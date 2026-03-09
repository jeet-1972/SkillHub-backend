package com.skillhub.lms.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class CourseDetailDto {
    private Long id;
    private String title;
    private String description;
    private String thumbnailUrl;
    private String category;
    private String whatYouLearn;
    private String instructorName;
    private Long instructorId;
    private Integer totalLessons;
    private Integer totalDurationMinutes;
    private BigDecimal price;
    private List<SectionSummaryDto> curriculum;

    @Data
    @Builder
    public static class SectionSummaryDto {
        private String title;
        private Integer lessonCount;
        private Integer totalMinutes;
    }
}
