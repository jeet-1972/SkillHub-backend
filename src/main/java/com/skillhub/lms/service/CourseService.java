package com.skillhub.lms.service;

import com.skillhub.lms.dto.*;
import com.skillhub.lms.entity.Course;
import com.skillhub.lms.entity.Section;
import com.skillhub.lms.entity.Video;
import com.skillhub.lms.repository.CourseRepository;
import com.skillhub.lms.repository.SectionRepository;
import com.skillhub.lms.repository.VideoProgressRepository;
import com.skillhub.lms.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;
    private final SectionRepository sectionRepository;
    private final VideoRepository videoRepository;
    private final VideoProgressRepository videoProgressRepository;

    @Transactional(readOnly = true)
    public List<CourseListItemDto> listCourses(String category, String search) {
        List<Course> courses;
        if (search != null && !search.isBlank()) {
            courses = courseRepository.searchByTitleOrDescription(search.trim());
        } else if (category != null && !category.isBlank()) {
            courses = courseRepository.findByCategory(category);
        } else {
            courses = courseRepository.findAll();
        }
        return courses.stream()
                .filter(c -> Boolean.TRUE.equals(c.getIsPublished()))
                .map(this::toListItem)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public LessonDto getVideoAsLessonDto(Long videoId, Long userId) {
        Video video = videoRepository.findByIdWithSectionAndCourse(videoId)
                .orElseThrow(() -> new IllegalArgumentException("Video not found"));
        Integer durationMinutes = video.getDurationSeconds() != null ? video.getDurationSeconds() / 60 : null;
        return LessonDto.builder()
                .id(video.getId())
                .title(video.getTitle())
                .sortOrder(video.getOrderIndex())
                .youtubeUrl(video.getYoutubeUrl())
                .durationMinutes(durationMinutes)
                .build();
    }

    @Transactional(readOnly = true)
    public CourseDetailDto getCourseDetail(Long id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Course not found"));
        List<Section> sections = sectionRepository.findByCourseIdWithVideos(id);
        List<Video> allVideos = videoRepository.findByCourseIdOrderBySectionAndOrder(id);
        int totalDuration = allVideos.stream()
                .mapToInt(v -> v.getDurationSeconds() != null ? v.getDurationSeconds() / 60 : 0)
                .sum();
        List<CourseDetailDto.SectionSummaryDto> curriculum = sections.stream()
                .map(s -> {
                    int sectionMinutes = s.getVideos().stream()
                            .mapToInt(v -> v.getDurationSeconds() != null ? v.getDurationSeconds() / 60 : 0)
                            .sum();
                    return CourseDetailDto.SectionSummaryDto.builder()
                            .title(s.getTitle())
                            .lessonCount(s.getVideos().size())
                            .totalMinutes(sectionMinutes)
                            .build();
                })
                .collect(Collectors.toList());
        return CourseDetailDto.builder()
                .id(course.getId())
                .title(course.getTitle())
                .description(course.getDescription())
                .thumbnailUrl(course.getThumbnailUrl())
                .category(course.getCategory())
                .whatYouLearn(course.getWhatYouLearn())
                .instructorName(course.getInstructor().getName())
                .instructorId(course.getInstructor().getId())
                .totalLessons(allVideos.size())
                .totalDurationMinutes(totalDuration)
                .price(course.getPrice())
                .curriculum(curriculum)
                .build();
    }

    @Transactional(readOnly = true)
    public List<SectionDto> getSectionsForCourse(Long courseId, Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("Unauthorized");
        }
        List<Section> sections = sectionRepository.findByCourseIdWithVideos(courseId);
        return sections.stream().map(section -> {
            List<LessonDto> lessons = section.getVideos().stream()
                    .sorted((a, b) -> Integer.compare(a.getOrderIndex(), b.getOrderIndex()))
                    .map(v -> LessonDto.builder()
                            .id(v.getId())
                            .title(v.getTitle())
                            .sortOrder(v.getOrderIndex())
                            .youtubeUrl(v.getYoutubeUrl())
                            .durationMinutes(v.getDurationSeconds() != null ? v.getDurationSeconds() / 60 : null)
                            .build())
                    .collect(Collectors.toList());
            return SectionDto.builder()
                    .id(section.getId())
                    .title(section.getTitle())
                    .sortOrder(section.getSortOrder())
                    .lessons(lessons)
                    .build();
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CourseTreeDto getCourseTree(Long courseId, Long userId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found"));
        List<Section> sections = sectionRepository.findByCourseIdWithVideos(courseId);
        List<Video> orderedVideos = videoRepository.findByCourseIdOrderBySectionAndOrder(courseId);
        Set<Long> completedIds = videoProgressRepository.findCompletedByUserIdAndCourseId(userId, courseId).stream()
                .map(vp -> vp.getVideo().getId())
                .collect(Collectors.toSet());
        List<SectionTreeDto> sectionDtos = sections.stream()
                .map(s -> {
                    List<VideoTreeItemDto> videoItems = s.getVideos().stream()
                            .sorted((a, b) -> Integer.compare(a.getOrderIndex(), b.getOrderIndex()))
                            .map(v -> {
                                int idx = indexInOrdered(orderedVideos, v.getId());
                                boolean locked = idx > 0 && !completedIds.contains(orderedVideos.get(idx - 1).getId());
                                boolean completed = completedIds.contains(v.getId());
                                return VideoTreeItemDto.builder()
                                        .id(v.getId())
                                        .title(v.getTitle())
                                        .orderIndex(v.getOrderIndex())
                                        .isCompleted(completed)
                                        .locked(locked)
                                        .build();
                            })
                            .collect(Collectors.toList());
                    return SectionTreeDto.builder()
                            .id(s.getId())
                            .title(s.getTitle())
                            .orderIndex(s.getSortOrder())
                            .videos(videoItems)
                            .build();
                })
                .collect(Collectors.toList());
        return CourseTreeDto.builder()
                .id(course.getId())
                .title(course.getTitle())
                .sections(sectionDtos)
                .build();
    }

    @Transactional(readOnly = true)
    public Long getFirstUnlockedVideoId(Long courseId, Long userId) {
        List<Video> ordered = videoRepository.findByCourseIdOrderBySectionAndOrder(courseId);
        if (ordered.isEmpty()) return null;
        Set<Long> completedIds = videoProgressRepository.findCompletedByUserIdAndCourseId(userId, courseId).stream()
                .map(vp -> vp.getVideo().getId())
                .collect(Collectors.toSet());
        for (int i = 0; i < ordered.size(); i++) {
            if (i == 0 || completedIds.contains(ordered.get(i - 1).getId())) {
                return ordered.get(i).getId();
            }
        }
        return ordered.get(0).getId();
    }

    /** Global ordered list of videos for course (for prev/next and locking). */
    List<Video> getOrderedVideosForCourse(Long courseId) {
        return videoRepository.findByCourseIdOrderBySectionAndOrder(courseId);
    }

    Set<Long> getCompletedVideoIdsForUser(Long userId, Long courseId) {
        return videoProgressRepository.findCompletedByUserIdAndCourseId(userId, courseId).stream()
                .map(vp -> vp.getVideo().getId())
                .collect(Collectors.toSet());
    }

    boolean isVideoUnlocked(Long videoId, Long userId, Long courseId) {
        List<Video> ordered = videoRepository.findByCourseIdOrderBySectionAndOrder(courseId);
        Set<Long> completed = getCompletedVideoIdsForUser(userId, courseId);
        int idx = indexInOrdered(ordered, videoId);
        if (idx < 0) return false;
        return idx == 0 || completed.contains(ordered.get(idx - 1).getId());
    }

    private static int indexInOrdered(List<Video> ordered, Long videoId) {
        for (int i = 0; i < ordered.size(); i++) {
            if (ordered.get(i).getId().equals(videoId)) return i;
        }
        return -1;
    }

    private CourseListItemDto toListItem(Course c) {
        String shortDesc = c.getDescription();
        if (shortDesc != null && shortDesc.length() > 200) {
            shortDesc = shortDesc.substring(0, 197) + "...";
        }
        return CourseListItemDto.builder()
                .id(c.getId())
                .title(c.getTitle())
                .thumbnailUrl(c.getThumbnailUrl())
                .instructorName(c.getInstructor().getName())
                .shortDescription(shortDesc)
                .category(c.getCategory())
                .price(c.getPrice())
                .build();
    }
}
