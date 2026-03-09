package com.skillhub.lms.config;

import com.skillhub.lms.entity.Course;
import com.skillhub.lms.entity.Section;
import com.skillhub.lms.entity.Video;
import com.skillhub.lms.repository.CourseRepository;
import com.skillhub.lms.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * One-time fix: replace known broken/unavailable YouTube video IDs in videos with working ones.
 * Also updates course thumbnail when the first video is fixed.
 */
@Component
@Order(2)
@RequiredArgsConstructor
@Slf4j
public class BrokenVideoUrlFix implements ApplicationRunner {

    /** Replace unavailable/restricted YouTube video IDs with known embeddable ones. */
    private static final Map<String, String> REPLACEMENTS = Map.ofEntries(
            Map.entry("eWR9ZV0b0Ic", "grEKMHGYyns"),
            Map.entry("8jOxkL7YF1U", "r59xYe3Vyks"),
            Map.entry("WPdWvnAAurg", "l1sIDd1F2xE"),
            Map.entry("C8Uw2ElVx7s", "Tn6-PIqc4UM"),
            Map.entry("oHpYzLqyHqA", "RGOj5yH7evk")
    );

    private final VideoRepository videoRepository;
    private final CourseRepository courseRepository;

    @Override
    @Transactional
    public void run(org.springframework.boot.ApplicationArguments args) {
        List<Video> all = videoRepository.findAll();
        int updated = 0;
        for (Video video : all) {
            String url = video.getYoutubeUrl();
            if (url == null) continue;
            for (Map.Entry<String, String> e : REPLACEMENTS.entrySet()) {
                if (url.contains(e.getKey())) {
                    String newVideoId = e.getValue();
                    String newUrl = url.replace(e.getKey(), newVideoId);
                    video.setYoutubeUrl(newUrl);
                    videoRepository.save(video);
                    updated++;
                    log.info("Fixed video '{}' URL (replaced unavailable video ID)", video.getTitle());
                    Section section = video.getSection();
                    if (section != null && Integer.valueOf(0).equals(video.getOrderIndex())) {
                        Course course = section.getCourse();
                        if (course != null) {
                            course.setThumbnailUrl("https://img.youtube.com/vi/" + newVideoId + "/mqdefault.jpg");
                            courseRepository.save(course);
                        }
                    }
                    break;
                }
            }
        }
        if (updated > 0) {
            log.info("Updated {} video(s) with working YouTube URLs.", updated);
        }
        backfillCourseThumbnailsFromFirstVideo();
    }

    /** Set course thumbnail from first video when thumbnail is missing or blank. */
    private void backfillCourseThumbnailsFromFirstVideo() {
        List<Course> courses = courseRepository.findAll();
        for (Course course : courses) {
            if (course.getThumbnailUrl() != null && !course.getThumbnailUrl().isBlank()) continue;
            List<Video> ordered = videoRepository.findByCourseIdOrderBySectionAndOrder(course.getId());
            if (ordered.isEmpty()) continue;
            String url = ordered.get(0).getYoutubeUrl();
            if (url == null) continue;
            String id = extractYouTubeVideoId(url);
            if (id != null) {
                course.setThumbnailUrl("https://img.youtube.com/vi/" + id + "/mqdefault.jpg");
                courseRepository.save(course);
                log.info("Set thumbnail for course '{}' from first video.", course.getTitle());
            }
        }
    }

    private static String extractYouTubeVideoId(String url) {
        if (url == null) return null;
        int i = url.indexOf("v=");
        if (i < 0) return null;
        i += 2;
        int end = url.indexOf('&', i) > 0 ? url.indexOf('&', i) : url.length();
        if (i >= url.length() || end > url.length()) return null;
        String id = url.substring(i, end).trim();
        return (id.length() == 11 && id.matches("[a-zA-Z0-9_-]+")) ? id : null;
    }
}
