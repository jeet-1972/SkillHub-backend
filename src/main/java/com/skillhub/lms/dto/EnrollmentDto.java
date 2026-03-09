package com.skillhub.lms.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class EnrollmentDto {
    private Long id;
    private Long courseId;
    private String courseTitle;
    private String thumbnailUrl;
    private String instructorName;
    private Instant enrolledAt;
}
