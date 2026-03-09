package com.skillhub.lms.controller;

import com.skillhub.lms.dto.CourseDetailDto;
import com.skillhub.lms.dto.CourseListItemDto;
import com.skillhub.lms.dto.CourseTreeDto;
import com.skillhub.lms.dto.FirstVideoResponseDto;
import com.skillhub.lms.dto.LessonDto;
import com.skillhub.lms.dto.SectionDto;
import com.skillhub.lms.security.UserPrincipal;
import com.skillhub.lms.service.CourseService;
import com.skillhub.lms.service.EnrollmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;
    private final EnrollmentService enrollmentService;

    @GetMapping
    public List<CourseListItemDto> list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search) {
        return courseService.listCourses(category, search);
    }

    @GetMapping("/{id}")
    public CourseDetailDto getDetail(@PathVariable Long id) {
        return courseService.getCourseDetail(id);
    }

    @GetMapping("/{courseId}/tree")
    public CourseTreeDto getTree(@PathVariable Long courseId, @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        if (!enrollmentService.isEnrolled(principal.getUserId(), courseId)) {
            throw new IllegalArgumentException("Not enrolled in this course");
        }
        return courseService.getCourseTree(courseId, principal.getUserId());
    }

    @GetMapping("/{courseId}/first-video")
    public FirstVideoResponseDto getFirstVideo(@PathVariable Long courseId, @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        if (!enrollmentService.isEnrolled(principal.getUserId(), courseId)) {
            throw new IllegalArgumentException("Not enrolled in this course");
        }
        Long videoId = courseService.getFirstUnlockedVideoId(courseId, principal.getUserId());
        return new FirstVideoResponseDto(videoId);
    }

    @GetMapping("/{id}/sections")
    public List<SectionDto> getSections(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            throw new IllegalArgumentException("Unauthorized");
        }
        if (!enrollmentService.isEnrolled(principal.getUserId(), id)) {
            throw new IllegalArgumentException("Not enrolled in this course");
        }
        return courseService.getSectionsForCourse(id, principal.getUserId());
    }

    @GetMapping("/{courseId}/enrolled")
    public boolean isEnrolled(@PathVariable Long courseId, @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) return false;
        return enrollmentService.isEnrolled(principal.getUserId(), courseId);
    }
}
