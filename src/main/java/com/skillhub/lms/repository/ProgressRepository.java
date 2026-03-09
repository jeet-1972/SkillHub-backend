package com.skillhub.lms.repository;

import com.skillhub.lms.entity.Progress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProgressRepository extends JpaRepository<Progress, Long> {

    Optional<Progress> findByUser_IdAndLesson_Id(Long userId, Long lessonId);

    List<Progress> findByUserIdAndCourseId(Long userId, Long courseId);

    @Query("SELECT p.lesson.id FROM Progress p WHERE p.user.id = :userId AND p.course.id = :courseId AND p.status = 'COMPLETED'")
    List<Long> findCompletedLessonIdsByUserIdAndCourseId(Long userId, Long courseId);

    @Query("SELECT p FROM Progress p WHERE p.user.id = :userId AND p.course.id = :courseId ORDER BY p.lastWatchedAt DESC")
    List<Progress> findLatestWatchedByUserIdAndCourseId(Long userId, Long courseId);

    @Query("SELECT p.lesson.id FROM Progress p WHERE p.user.id = :userId AND p.course.id = :courseId AND p.lastWatchedAt IS NOT NULL ORDER BY p.lastWatchedAt DESC")
    List<Long> findLastWatchedLessonIdsByUserIdAndCourseId(Long userId, Long courseId);
}
