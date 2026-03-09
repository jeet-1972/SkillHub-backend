package com.skillhub.lms.config;

import com.skillhub.lms.repository.CourseRepository;
import com.skillhub.lms.repository.EnrollmentRepository;
import com.skillhub.lms.repository.VideoProgressRepository;
import com.skillhub.lms.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * One-time cleanup: remove the legacy combined "DevOps & Java DSA" course so that
 * SampleDataInitializer and/or YouTube seed can create the two separate courses
 * (DevOps Bootcamp, Java + DSA + Interview Preparation).
 * Deletes video_progress, then enrollments, then the course to satisfy FK constraints.
 */
@Component
@Order(-1)
@RequiredArgsConstructor
@Slf4j
public class LegacyCourseCleanupRunner implements ApplicationRunner {

    private static final String LEGACY_SLUG = "devops-java-dsa";

    private final CourseRepository courseRepository;
    private final VideoProgressRepository videoProgressRepository;
    private final VideoRepository videoRepository;
    private final EnrollmentRepository enrollmentRepository;

    @Override
    @Transactional
    public void run(org.springframework.boot.ApplicationArguments args) {
        courseRepository.findBySlug(LEGACY_SLUG)
                .ifPresent(c -> {
                    Long courseId = c.getId();
                    List<Long> videoIds = videoRepository.findByCourseIdOrderBySectionAndOrder(courseId).stream()
                            .map(v -> v.getId())
                            .collect(Collectors.toList());
                    if (!videoIds.isEmpty()) {
                        videoProgressRepository.deleteByVideo_IdIn(videoIds);
                    }
                    enrollmentRepository.deleteByCourse_Id(courseId);
                    courseRepository.delete(c);
                    log.info("Removed legacy combined course (slug: {}); use separate DevOps Bootcamp and Java + DSA courses.", LEGACY_SLUG);
                });
    }
}
