package com.skillhub.lms.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SectionDto {
    private Long id;
    private String title;
    private Integer sortOrder;
    private List<LessonDto> lessons;
}
