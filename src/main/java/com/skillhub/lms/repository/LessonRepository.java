package com.skillhub.lms.repository;

import com.skillhub.lms.entity.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface LessonRepository extends JpaRepository<Lesson, Long> {

    List<Lesson> findBySectionIdOrderBySortOrderAsc(Long sectionId);

    @Query("SELECT l FROM Lesson l JOIN l.section s WHERE s.course.id = :courseId ORDER BY s.sortOrder, l.sortOrder")
    List<Lesson> findByCourseIdOrderBySectionAndOrder(Long courseId);

    @Query("SELECT l FROM Lesson l JOIN FETCH l.section s JOIN FETCH s.course c WHERE l.id = :lessonId")
    Optional<Lesson> findByIdWithSectionAndCourse(Long lessonId);
}
