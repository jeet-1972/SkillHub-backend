package com.skillhub.lms.config;

import com.skillhub.lms.entity.Course;
import com.skillhub.lms.entity.Lesson;
import com.skillhub.lms.entity.Progress;
import com.skillhub.lms.entity.Video;
import com.skillhub.lms.entity.VideoProgress;
import com.skillhub.lms.repository.CourseRepository;
import com.skillhub.lms.repository.LessonRepository;
import com.skillhub.lms.repository.ProgressRepository;
import com.skillhub.lms.repository.VideoProgressRepository;
import com.skillhub.lms.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * One-time migration: copy lessons -> videos and progress -> video_progress.
 * Runs only when videos table is empty and lessons exist (e.g. existing DB before schema change).
 */
@Component
@Order(-100)
@RequiredArgsConstructor
@Slf4j
@Profile("!seed")
public class LessonToVideoMigrationRunner implements ApplicationRunner {

    private final LessonRepository lessonRepository;
    private final ProgressRepository progressRepository;
    private final VideoRepository videoRepository;
    private final VideoProgressRepository videoProgressRepository;
    private final CourseRepository courseRepository;

    @Override
    @Transactional
    public void run(org.springframework.boot.ApplicationArguments args) {
        if (videoRepository.count() > 0) {
            fixExistingCoursesSlugAndPublished();
            return;
        }
        long lessonCount = lessonRepository.count();
        if (lessonCount == 0) {
            return;
        }
        log.info("Migrating {} lessons to videos...", lessonCount);
        List<Lesson> lessons = lessonRepository.findAll();
        for (Lesson lesson : lessons) {
            Video video = Video.builder()
                    .section(lesson.getSection())
                    .title(lesson.getTitle())
                    .description(null)
                    .youtubeUrl(lesson.getYoutubeUrl())
                    .orderIndex(lesson.getSortOrder())
                    .durationSeconds(lesson.getDurationMinutes() != null ? lesson.getDurationMinutes() * 60 : null)
                    .build();
            videoRepository.save(video);
        }
        log.info("Migrated lessons to videos.");

        long progressCount = progressRepository.count();
        if (progressCount > 0) {
            log.info("Migrating {} progress rows to video_progress...", progressCount);
            for (Progress p : progressRepository.findAll()) {
                Lesson lesson = p.getLesson();
                videoRepository.findBySection_IdAndOrderIndex(lesson.getSection().getId(), lesson.getSortOrder())
                        .ifPresent(video -> {
                            VideoProgress vp = VideoProgress.builder()
                                    .user(p.getUser())
                                    .video(video)
                                    .lastPositionSeconds(0)
                                    .isCompleted(p.getStatus() == Progress.Status.COMPLETED)
                                    .completedAt(p.getCompletedAt())
                                    .build();
                            videoProgressRepository.save(vp);
                        });
            }
            log.info("Migrated progress to video_progress.");
        }
        fixExistingCoursesSlugAndPublished();
    }

    private void fixExistingCoursesSlugAndPublished() {
        courseRepository.findAll().forEach(course -> {
            boolean changed = false;
            if (course.getSlug() == null || course.getSlug().isBlank()) {
                course.setSlug(slugFromTitle(course.getTitle()));
                changed = true;
            }
            if (course.getIsPublished() == null || !Boolean.TRUE.equals(course.getIsPublished())) {
                course.setIsPublished(true);
                changed = true;
            }
            if (changed) courseRepository.save(course);
        });
    }

    private static String slugFromTitle(String title) {
        if (title == null || title.isBlank()) return "course";
        return title.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .trim();
    }
}
