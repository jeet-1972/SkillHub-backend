package com.skillhub.lms.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class CourseListItemDto {
    private Long id;
    private String title;
    private String thumbnailUrl;
    private String instructorName;
    private String shortDescription;
    private String category;
    private BigDecimal price;
}
