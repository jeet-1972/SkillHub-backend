package com.skillhub.lms.seed;

import com.skillhub.lms.config.SeedProperties;
import com.skillhub.lms.entity.Course;
import com.skillhub.lms.entity.Section;
import com.skillhub.lms.entity.User;
import com.skillhub.lms.entity.Video;
import com.skillhub.lms.repository.CourseRepository;
import com.skillhub.lms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Creates courses (and sections/lessons) from YouTube playlist IDs using the YouTube Data API.
 * Used by both the seed-profile runner and the bootstrap runner (normal startup).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class YouTubeSeedService {

    private final YouTubeApiClient youTubeApiClient;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final SeedProperties seedProperties;
    private final PasswordEncoder passwordEncoder;

    /** Category by playlist ID for better UI (optional). */
    private static final Map<String, String> CATEGORY_BY_PLAYLIST = Map.of(
            "PLfqMhTWNBTe0PY9xunOzsP5kmYIz2Hu7i", "Web Development",
            "PLxCzCOWd7aiH2neELedJ9eXc8iR49oNVV", "Design",
            "PLxCzCOWd7aiFbom4rYyl5qROgqYuCuAPD", "Database",
            "PLxCzCOWd7aiGz9donHRrE9I3Mwn6XdP8p", "Systems",
            "PL9gnSGHSqcnoqBXdMwUTRod4Gi3eac2Ak", "Development",
            "PL9gnSGHSqcnr_DxHsP7AW9ftq0AtAyYqJ", "Programming"
    );

    @Transactional
    public void runSeed() {
        List<String> playlistIds = seedProperties.getPlaylistIds();
        if (playlistIds == null || playlistIds.isEmpty()) {
            log.debug("No playlist IDs configured. Set app.seed.playlistIds in application.yml");
            return;
        }
        User instructor = userRepository.findByEmail(seedProperties.getDefaultInstructorEmail())
                .orElseGet(() -> {
                    User u = User.builder()
                            .name("SkillHub Instructor")
                            .email(seedProperties.getDefaultInstructorEmail())
                            .username(seedProperties.getDefaultInstructorEmail())
                            .phone("")
                            .passwordHash(passwordEncoder.encode("changeme"))
                            .role(User.Role.INSTRUCTOR)
                            .build();
                    return userRepository.save(u);
                });

        for (String playlistId : playlistIds) {
            String id = playlistId.trim();
            if (id.isEmpty()) continue;
            try {
                YouTubeApiClient.PlaylistInfo info = youTubeApiClient.getPlaylistInfo(id);
                if (info == null) {
                    log.warn("Playlist not found: {}", id);
                    continue;
                }
                if (courseRepository.findAll().stream().anyMatch(c -> id.equals(c.getYoutubePlaylistId()))) {
                    log.debug("Course already exists for playlist: {}", id);
                    continue;
                }
                List<YouTubeApiClient.VideoInfo> videos = youTubeApiClient.getPlaylistVideos(id);
                if (videos.isEmpty()) {
                    log.warn("No videos in playlist: {}", id);
                    continue;
                }
                String slug = slugFromTitle(info.title());
                String firstVideoId = videos.get(0).videoId;
                String thumbnailUrl = "https://img.youtube.com/vi/" + firstVideoId + "/mqdefault.jpg";
                String category = CATEGORY_BY_PLAYLIST.getOrDefault(id, "Programming");

                Course course = courseRepository.findBySlug(slug)
                        .filter(c -> c.getYoutubePlaylistId() == null || c.getYoutubePlaylistId().isBlank())
                        .orElse(null);

                if (course != null) {
                    course.setTitle(info.title());
                    course.setDescription("Course created from YouTube playlist: " + info.title());
                    course.setThumbnailUrl(thumbnailUrl);
                    course.setCategory(category);
                    course.setYoutubePlaylistId(id);
                    course.getSections().clear();
                    course = courseRepository.save(course);
                    log.info("Updating existing course by slug '{}' with {} videos from playlist {}", slug, videos.size(), id);
                } else {
                    course = Course.builder()
                            .title(info.title())
                            .slug(slug)
                            .description("Course created from YouTube playlist: " + info.title())
                            .thumbnailUrl(thumbnailUrl)
                            .category(category)
                            .whatYouLearn("Learn from the playlist videos.")
                            .instructor(instructor)
                            .price(BigDecimal.ZERO)
                            .youtubePlaylistId(id)
                            .isPublished(true)
                            .build();
                    course = courseRepository.save(course);
                    log.info("Created course '{}' with {} videos from playlist {}", info.title(), videos.size(), id);
                }

                Section section = Section.builder()
                        .course(course)
                        .title("Videos")
                        .sortOrder(0)
                        .build();
                course.getSections().add(section);

                for (int i = 0; i < videos.size(); i++) {
                    YouTubeApiClient.VideoInfo v = videos.get(i);
                    int durationSeconds = v.durationMinutes != null ? v.durationMinutes * 60 : 0;
                    Video video = Video.builder()
                            .section(section)
                            .title(v.title)
                            .youtubeUrl("https://www.youtube.com/watch?v=" + v.videoId)
                            .orderIndex(i)
                            .durationSeconds(durationSeconds > 0 ? durationSeconds : null)
                            .build();
                    section.getVideos().add(video);
                }
                courseRepository.save(course);
            } catch (Exception e) {
                log.error("Failed to seed playlist {}", id, e);
            }
        }
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
