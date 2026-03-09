package com.skillhub.lms.service;

import com.skillhub.lms.dto.CourseProgressDto;
import com.skillhub.lms.dto.VideoProgressResponseDto;
import com.skillhub.lms.entity.User;
import com.skillhub.lms.entity.Video;
import com.skillhub.lms.entity.VideoProgress;
import com.skillhub.lms.repository.EnrollmentRepository;
import com.skillhub.lms.repository.VideoProgressRepository;
import com.skillhub.lms.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProgressService {

    private final VideoProgressRepository videoProgressRepository;
    private final VideoRepository videoRepository;
    private final EnrollmentRepository enrollmentRepository;

    @Transactional
    public CourseProgressDto markComplete(Long userId, Long videoId) {
        Video video = videoRepository.findByIdWithSectionAndCourse(videoId)
                .orElseThrow(() -> new IllegalArgumentException("Video not found"));
        Long courseId = video.getSection().getCourse().getId();
        if (!enrollmentRepository.existsByUserIdAndCourseId(userId, courseId)) {
            throw new IllegalArgumentException("Not enrolled");
        }
        Optional<VideoProgress> existingOpt = videoProgressRepository.findByUser_IdAndVideo_Id(userId, videoId);
        if (existingOpt.isPresent()) {
            VideoProgress existing = existingOpt.get();
            existing.setCompletedAt(Instant.now());
            existing.setIsCompleted(true);
            videoProgressRepository.save(existing);
        } else {
            User userRef = new User();
            userRef.setId(userId);
            videoProgressRepository.save(VideoProgress.builder()
                    .user(userRef)
                    .video(video)
                    .lastPositionSeconds(0)
                    .isCompleted(true)
                    .completedAt(Instant.now())
                    .build());
        }
        return getCourseProgress(userId, courseId);
    }

    @Transactional
    public void updateLastWatched(Long userId, Long videoId) {
        if (userId == null || videoId == null) return;
        Optional<Video> videoOpt = videoRepository.findByIdWithSectionAndCourse(videoId);
        if (videoOpt.isEmpty()) return;
        Video video = videoOpt.get();
        Long courseId = video.getSection().getCourse().getId();
        if (!enrollmentRepository.existsByUserIdAndCourseId(userId, courseId)) return;
        Optional<VideoProgress> existingOpt = videoProgressRepository.findByUser_IdAndVideo_Id(userId, videoId);
        if (existingOpt.isPresent()) {
            videoProgressRepository.save(existingOpt.get());
        } else {
            User userRef = new User();
            userRef.setId(userId);
            videoProgressRepository.save(VideoProgress.builder()
                    .user(userRef)
                    .video(video)
                    .lastPositionSeconds(0)
                    .isCompleted(false)
                    .build());
        }
    }

    @Transactional(readOnly = true)
    public CourseProgressDto getCourseProgress(Long userId, Long courseId) {
        List<VideoProgress> completed = videoProgressRepository.findCompletedByUserIdAndCourseId(userId, courseId);
        List<Video> allVideos = videoRepository.findByCourseIdOrderBySectionAndOrder(courseId);
        int total = allVideos.size();
        int completedCount = completed.size();
        int percentage = total > 0 ? (completedCount * 100) / total : 0;
        List<VideoProgress> latestWatched = videoProgressRepository.findLatestWatchedByUserIdAndCourseId(userId, courseId);
        Long lastWatchedId = null;
        Integer lastPositionSeconds = null;
        if (!latestWatched.isEmpty()) {
            VideoProgress last = latestWatched.get(0);
            lastWatchedId = last.getVideo().getId();
            lastPositionSeconds = last.getLastPositionSeconds();
        } else if (!allVideos.isEmpty()) {
            lastWatchedId = allVideos.get(0).getId();
        }
        List<Long> completedIds = completed.stream()
                .map(vp -> vp.getVideo().getId())
                .collect(Collectors.toList());
        return CourseProgressDto.builder()
                .courseId(courseId)
                .completedLessonIds(completedIds)
                .percentage(percentage)
                .lastWatchedLessonId(lastWatchedId)
                .lastPositionSeconds(lastPositionSeconds)
                .totalLessons(total)
                .completedCount(completedCount)
                .build();
    }

    @Transactional(readOnly = true)
    public VideoProgressResponseDto getVideoProgress(Long userId, Long videoId) {
        return videoProgressRepository.findByUser_IdAndVideo_Id(userId, videoId)
                .map(vp -> VideoProgressResponseDto.builder()
                        .lastPositionSeconds(vp.getLastPositionSeconds() != null ? vp.getLastPositionSeconds() : 0)
                        .isCompleted(Boolean.TRUE.equals(vp.getIsCompleted()))
                        .build())
                .orElse(VideoProgressResponseDto.builder().lastPositionSeconds(0).isCompleted(false).build());
    }

    @Transactional
    public VideoProgressResponseDto upsertVideoProgress(Long userId, Long videoId, Integer lastPositionSeconds, Boolean isCompleted) {
        Video video = videoRepository.findByIdWithSectionAndCourse(videoId)
                .orElseThrow(() -> new IllegalArgumentException("Video not found"));
        Long courseId = video.getSection().getCourse().getId();
        if (!enrollmentRepository.existsByUserIdAndCourseId(userId, courseId)) {
            throw new IllegalArgumentException("Not enrolled");
        }
        int position = lastPositionSeconds != null && lastPositionSeconds >= 0 ? lastPositionSeconds : 0;
        if (video.getDurationSeconds() != null && position > video.getDurationSeconds()) {
            position = video.getDurationSeconds();
        }
        VideoProgress vp = videoProgressRepository.findByUser_IdAndVideo_Id(userId, videoId).orElse(null);
        if (vp == null) {
            User userRef = new User();
            userRef.setId(userId);
            vp = VideoProgress.builder()
                    .user(userRef)
                    .video(video)
                    .lastPositionSeconds(position)
                    .isCompleted(Boolean.TRUE.equals(isCompleted))
                    .build();
        } else {
            vp.setLastPositionSeconds(position);
            if (isCompleted != null) {
                vp.setIsCompleted(isCompleted);
            }
        }
        vp = videoProgressRepository.save(vp);
        return VideoProgressResponseDto.builder()
                .lastPositionSeconds(vp.getLastPositionSeconds())
                .isCompleted(Boolean.TRUE.equals(vp.getIsCompleted()))
                .build();
    }

    @Transactional(readOnly = true)
    public List<CourseProgressDto> getAllProgress(Long userId) {
        return enrollmentRepository.findByUserIdWithCourseAndInstructor(userId).stream()
                .map(e -> getCourseProgress(userId, e.getCourse().getId()))
                .collect(Collectors.toList());
    }
}
