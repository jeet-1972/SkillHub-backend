package com.skillhub.lms.repository;

import com.skillhub.lms.entity.Section;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SectionRepository extends JpaRepository<Section, Long> {

    List<Section> findByCourseIdOrderBySortOrderAsc(Long courseId);

    @Query("SELECT s FROM Section s LEFT JOIN FETCH s.videos WHERE s.course.id = :courseId ORDER BY s.sortOrder")
    List<Section> findByCourseIdWithVideos(Long courseId);
}
