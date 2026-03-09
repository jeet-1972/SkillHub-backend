package com.skillhub.lms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoProgressResponseDto {
    private int lastPositionSeconds;
    private boolean isCompleted;
}
