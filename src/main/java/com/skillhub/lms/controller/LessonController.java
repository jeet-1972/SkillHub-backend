package com.skillhub.lms.controller;

import com.skillhub.lms.dto.LessonDto;
import com.skillhub.lms.repository.EnrollmentRepository;
import com.skillhub.lms.repository.VideoRepository;
import com.skillhub.lms.security.UserPrincipal;
import com.skillhub.lms.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/lessons")
@RequiredArgsConstructor
public class LessonController {

    private final CourseService courseService;
    private final EnrollmentRepository enrollmentRepository;
    private final VideoRepository videoRepository;

    @GetMapping("/{lessonId}")
    public LessonDto getLesson(@PathVariable Long lessonId, @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            throw new IllegalArgumentException("Unauthorized");
        }
        Long courseId = videoRepository.findByIdWithSectionAndCourse(lessonId)
                .map(v -> v.getSection().getCourse().getId())
                .orElseThrow(() -> new IllegalArgumentException("Lesson not found"));
        if (!enrollmentRepository.existsByUserIdAndCourseId(principal.getUserId(), courseId)) {
            throw new IllegalArgumentException("Not enrolled in this course");
        }
        return courseService.getVideoAsLessonDto(lessonId, principal.getUserId());
    }
}
