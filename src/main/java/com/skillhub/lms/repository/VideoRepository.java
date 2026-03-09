package com.skillhub.lms.repository;

import com.skillhub.lms.entity.Video;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface VideoRepository extends JpaRepository<Video, Long> {

    List<Video> findBySectionIdOrderByOrderIndexAsc(Long sectionId);

    @Query("SELECT v FROM Video v JOIN v.section s WHERE s.course.id = :courseId ORDER BY s.sortOrder, v.orderIndex")
    List<Video> findByCourseIdOrderBySectionAndOrder(Long courseId);

    @Query("SELECT v FROM Video v JOIN FETCH v.section s JOIN FETCH s.course c WHERE v.id = :videoId")
    Optional<Video> findByIdWithSectionAndCourse(Long videoId);

    Optional<Video> findBySection_IdAndOrderIndex(Long sectionId, Integer orderIndex);
}
