package com.skillhub.lms.controller;

import com.skillhub.lms.dto.VideoDetailDto;
import com.skillhub.lms.repository.EnrollmentRepository;
import com.skillhub.lms.repository.VideoRepository;
import com.skillhub.lms.security.UserPrincipal;
import com.skillhub.lms.service.VideoService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/videos")
@RequiredArgsConstructor
public class VideoController {

    private final VideoService videoService;
    private final VideoRepository videoRepository;
    private final EnrollmentRepository enrollmentRepository;

    @GetMapping("/{videoId}")
    public VideoDetailDto getVideo(@PathVariable Long videoId, @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            throw new IllegalArgumentException("Unauthorized");
        }
        Long courseId = videoRepository.findByIdWithSectionAndCourse(videoId)
                .map(v -> v.getSection().getCourse().getId())
                .orElseThrow(() -> new IllegalArgumentException("Video not found"));
        if (!enrollmentRepository.existsByUserIdAndCourseId(principal.getUserId(), courseId)) {
            throw new IllegalArgumentException("Not enrolled in this course");
        }
        return videoService.getVideoDetail(videoId, principal.getUserId());
    }
}
