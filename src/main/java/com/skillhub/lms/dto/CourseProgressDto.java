package com.skillhub.lms.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CourseProgressDto {
    private Long courseId;
    private List<Long> completedLessonIds;
    private int percentage;
    private Long lastWatchedLessonId;
    private Integer lastPositionSeconds;
    private int totalLessons;
    private int completedCount;
}
