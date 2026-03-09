package com.skillhub.lms.config;

import com.skillhub.lms.seed.YouTubeSeedService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * On normal startup (without "seed" profile): if YOUTUBE_API_KEY is set and playlist IDs
 * are configured, run the YouTube seed so playlist-based courses are created or updated.
 * This ensures the app has all courses from the configured playlists without requiring
 * a separate "seed" run.
 */
@Component
@Profile("!seed")
@Order(0)
@RequiredArgsConstructor
@Slf4j
public class BootstrapSeedRunner implements ApplicationRunner {

    private final YouTubeSeedService youTubeSeedService;
    private final SeedProperties seedProperties;

    @Value("${app.youtube.api-key:}")
    private String youtubeApiKey;

    @Override
    public void run(org.springframework.boot.ApplicationArguments args) {
        if (youtubeApiKey == null || youtubeApiKey.isBlank()) {
            log.info("YouTube API key not set; skipping bootstrap seed. Add app.youtube.api-key in application-local.yml to create courses from playlists.");
            return;
        }
        List<String> playlistIds = seedProperties.getPlaylistIds();
        if (playlistIds == null || playlistIds.isEmpty()) {
            log.info("No playlist IDs in app.seed.playlistIds; skipping bootstrap seed.");
            return;
        }
        log.info("YouTube API key set; running bootstrap seed for {} playlist(s) (courses will appear in UI when done).", playlistIds.size());
        try {
            youTubeSeedService.runSeed();
        } catch (Exception e) {
            log.error("Bootstrap seed failed. Only sample courses may show. Fix: check API key, enable YouTube Data API v3, and quota. Error: {}", e.getMessage(), e);
        }
    }
}
