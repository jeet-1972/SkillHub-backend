package com.skillhub.lms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoTreeItemDto {
    private Long id;
    private String title;
    private Integer orderIndex;
    private Boolean isCompleted;
    private Boolean locked;
}
