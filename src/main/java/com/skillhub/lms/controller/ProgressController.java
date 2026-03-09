package com.skillhub.lms.controller;

import com.skillhub.lms.dto.CourseProgressDto;
import com.skillhub.lms.dto.VideoProgressResponseDto;
import com.skillhub.lms.security.UserPrincipal;
import com.skillhub.lms.service.ProgressService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/progress")
@RequiredArgsConstructor
public class ProgressController {

    private final ProgressService progressService;

    @PostMapping("/complete")
    public CourseProgressDto markComplete(@RequestBody Map<String, Long> body, @AuthenticationPrincipal UserPrincipal principal) {
        Long videoId = body.get("lessonId");
        if (videoId == null) {
            videoId = body.get("videoId");
        }
        if (videoId == null) {
            throw new IllegalArgumentException("lessonId or videoId is required");
        }
        return progressService.markComplete(principal.getUserId(), videoId);
    }

    @PostMapping("/watching")
    public void updateLastWatched(@RequestBody(required = false) Map<String, Object> body, @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) return;
        if (body == null) return;
        Long videoId = null;
        if (body.get("lessonId") instanceof Number) videoId = ((Number) body.get("lessonId")).longValue();
        else if (body.get("videoId") instanceof Number) videoId = ((Number) body.get("videoId")).longValue();
        if (videoId != null) {
            progressService.updateLastWatched(principal.getUserId(), videoId);
        }
    }

    @GetMapping("/courses/{courseId}")
    public CourseProgressDto getCourseProgress(@PathVariable Long courseId, @AuthenticationPrincipal UserPrincipal principal) {
        return progressService.getCourseProgress(principal.getUserId(), courseId);
    }

    @GetMapping("/videos/{videoId}")
    public VideoProgressResponseDto getVideoProgress(@PathVariable Long videoId, @AuthenticationPrincipal UserPrincipal principal) {
        return progressService.getVideoProgress(principal.getUserId(), videoId);
    }

    @PostMapping("/videos/{videoId}")
    public VideoProgressResponseDto upsertVideoProgress(
            @PathVariable Long videoId,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        Integer lastPositionSeconds = body.get("last_position_seconds") != null
                ? ((Number) body.get("last_position_seconds")).intValue()
                : (body.get("lastPositionSeconds") != null ? ((Number) body.get("lastPositionSeconds")).intValue() : null);
        Boolean isCompleted = null;
        if (body.containsKey("is_completed")) isCompleted = (Boolean) body.get("is_completed");
        else if (body.containsKey("isCompleted")) isCompleted = (Boolean) body.get("isCompleted");
        return progressService.upsertVideoProgress(principal.getUserId(), videoId, lastPositionSeconds, isCompleted);
    }

    @GetMapping
    public List<CourseProgressDto> getAllProgress(@AuthenticationPrincipal UserPrincipal principal) {
        return progressService.getAllProgress(principal.getUserId());
    }
}
