package com.skillhub.lms.service;

import com.skillhub.lms.dto.VideoDetailDto;
import com.skillhub.lms.entity.Video;
import com.skillhub.lms.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VideoService {

    private final VideoRepository videoRepository;
    private final CourseService courseService;

    @Transactional(readOnly = true)
    public VideoDetailDto getVideoDetail(Long videoId, Long userId) {
        Video video = videoRepository.findByIdWithSectionAndCourse(videoId)
                .orElseThrow(() -> new IllegalArgumentException("Video not found"));
        Long courseId = video.getSection().getCourse().getId();
        List<Video> ordered = courseService.getOrderedVideosForCourse(courseId);
        int idx = -1;
        for (int i = 0; i < ordered.size(); i++) {
            if (ordered.get(i).getId().equals(videoId)) {
                idx = i;
                break;
            }
        }
        Long prevId = idx > 0 ? ordered.get(idx - 1).getId() : null;
        Long nextId = idx >= 0 && idx < ordered.size() - 1 ? ordered.get(idx + 1).getId() : null;
        boolean locked = !courseService.isVideoUnlocked(videoId, userId, courseId);
        String unlockReason = locked ? "Complete previous video" : null;
        return VideoDetailDto.builder()
                .id(video.getId())
                .title(video.getTitle())
                .description(video.getDescription())
                .youtubeUrl(video.getYoutubeUrl())
                .orderIndex(video.getOrderIndex())
                .durationSeconds(video.getDurationSeconds())
                .sectionId(video.getSection().getId())
                .sectionTitle(video.getSection().getTitle())
                .courseId(courseId)
                .courseTitle(video.getSection().getCourse().getTitle())
                .previousVideoId(prevId)
                .nextVideoId(nextId)
                .locked(locked)
                .unlockReason(unlockReason)
                .build();
    }
}
