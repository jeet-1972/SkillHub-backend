package com.skillhub.lms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SectionTreeDto {
    private Long id;
    private String title;
    private Integer orderIndex;
    private List<VideoTreeItemDto> videos;
}
