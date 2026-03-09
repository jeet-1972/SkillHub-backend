package com.skillhub.lms.repository;

import com.skillhub.lms.entity.VideoProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface VideoProgressRepository extends JpaRepository<VideoProgress, Long> {

    Optional<VideoProgress> findByUser_IdAndVideo_Id(Long userId, Long videoId);

    List<VideoProgress> findByUser_IdAndVideo_Section_Course_Id(Long userId, Long courseId);

    @Query("SELECT vp FROM VideoProgress vp WHERE vp.user.id = :userId AND vp.video.section.course.id = :courseId AND vp.isCompleted = true")
    List<VideoProgress> findCompletedByUserIdAndCourseId(Long userId, Long courseId);

    @Query("SELECT vp FROM VideoProgress vp WHERE vp.user.id = :userId AND vp.video.section.course.id = :courseId AND vp.completedAt IS NOT NULL ORDER BY vp.completedAt DESC")
    List<VideoProgress> findLatestWatchedByUserIdAndCourseId(Long userId, Long courseId);

    void deleteByVideo_IdIn(Iterable<Long> videoIds);
}
