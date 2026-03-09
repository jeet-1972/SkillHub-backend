package com.skillhub.lms.seed;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * YouTube Data API v3 client. Uses x-goog-api-key header for auth (per Google API key best practices).
 * @see <a href="https://docs.cloud.google.com/docs/authentication/api-keys-best-practices">Best practices for managing API keys</a>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class YouTubeApiClient {

    private static final String PLAYLIST_ITEMS_URL = "https://www.googleapis.com/youtube/v3/playlistItems?part=snippet,contentDetails&playlistId=%s&maxResults=50";
    private static final String PLAYLIST_URL = "https://www.googleapis.com/youtube/v3/playlists?part=snippet&id=%s";
    private static final String VIDEOS_URL = "https://www.googleapis.com/youtube/v3/videos?part=snippet,contentDetails&id=%s";
    private static final String KEY_PARAM = "&key=%s";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private RestTemplate restTemplate;

    @Value("${app.youtube.api-key:}")
    private String apiKey;

    private RestTemplate getRestTemplate() {
        if (restTemplate == null) {
            RestTemplate rt = new RestTemplate(new SimpleClientHttpRequestFactory());
            String key = this.apiKey;
            rt.setInterceptors(Collections.singletonList(new ClientHttpRequestInterceptor() {
                @Override
                public org.springframework.http.client.ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws java.io.IOException {
                    if (key != null && !key.isBlank()) {
                        request.getHeaders().set("x-goog-api-key", key);
                    }
                    return execution.execute(request, body);
                }
            }));
            this.restTemplate = rt;
        }
        return restTemplate;
    }

    public PlaylistInfo getPlaylistInfo(String playlistId) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("YouTube API key is not set (app.youtube.api-key or YOUTUBE_API_KEY)");
        }
        String url = String.format(PLAYLIST_URL + KEY_PARAM, playlistId, apiKey);
        String json;
        try {
            json = getRestTemplate().getForObject(url, String.class);
        } catch (RestClientException e) {
            log.warn("YouTube API request failed for playlist {}: {}. Check API key and that YouTube Data API v3 is enabled.", playlistId, e.getMessage());
            throw e;
        }
        if (json == null) return null;
        try {
            JsonNode root = objectMapper.readTree(json);
            if (root.has("error")) {
                JsonNode err = root.get("error");
                int code = err.path("code").asInt(0);
                String msg = err.path("message").asText("");
                log.warn("YouTube API error for playlist {}: code={}, message={}", playlistId, code, msg);
                return null;
            }
            JsonNode items = root.get("items");
            if (items == null || !items.isArray() || items.isEmpty()) return null;
            JsonNode item = items.get(0);
            String title = item.path("snippet").path("title").asText("");
            return new PlaylistInfo(playlistId, title);
        } catch (Exception e) {
            log.warn("Failed to parse playlist info for {}", playlistId, e);
            return null;
        }
    }

    public List<VideoInfo> getPlaylistVideos(String playlistId) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("YouTube API key is not set (app.youtube.api-key or YOUTUBE_API_KEY)");
        }
        List<VideoInfo> result = new ArrayList<>();
        String nextPageToken = null;
        do {
            String url = String.format(PLAYLIST_ITEMS_URL + KEY_PARAM, playlistId, apiKey);
            if (nextPageToken != null) {
                url += "&pageToken=" + nextPageToken;
            }
            String json = getRestTemplate().getForObject(url, String.class);
            if (json == null) break;
            try {
                JsonNode root = objectMapper.readTree(json);
                JsonNode items = root.get("items");
                if (items == null || !items.isArray()) break;
                for (JsonNode item : items) {
                    JsonNode snippet = item.get("snippet");
                    JsonNode contentDetails = item.get("contentDetails");
                    if (snippet == null || contentDetails == null) continue;
                    String videoId = contentDetails.path("videoId").asText(null);
                    if (videoId == null) continue;
                    String title = snippet.path("title").asText("Untitled");
                    result.add(new VideoInfo(videoId, title, null));
                }
                nextPageToken = root.has("nextPageToken") ? root.path("nextPageToken").asText() : null;
            } catch (Exception e) {
                log.warn("Failed to parse playlist items", e);
                break;
            }
        } while (nextPageToken != null);

        if (!result.isEmpty()) {
            fetchDurations(result);
        }
        return result;
    }

    private void fetchDurations(List<VideoInfo> videos) {
        for (int i = 0; i < videos.size(); i += 50) {
            int end = Math.min(i + 50, videos.size());
            List<VideoInfo> batch = videos.subList(i, end);
            String ids = batch.stream().map(v -> v.videoId).reduce((a, b) -> a + "," + b).orElse("");
            String url = String.format(VIDEOS_URL + KEY_PARAM, ids, apiKey);
            try {
                String json = getRestTemplate().getForObject(url, String.class);
                if (json == null) continue;
                JsonNode root = objectMapper.readTree(json);
                JsonNode items = root.get("items");
                if (items == null || !items.isArray()) continue;
                for (int j = 0; j < items.size() && (i + j) < videos.size(); j++) {
                    JsonNode item = items.get(j);
                    String duration = item.path("contentDetails").path("duration").asText("");
                    videos.get(i + j).durationMinutes = parseDurationToMinutes(duration);
                }
            } catch (Exception e) {
                log.warn("Failed to fetch video durations", e);
            }
        }
    }

    private static int parseDurationToMinutes(String iso8601) {
        if (iso8601 == null || iso8601.isEmpty()) return 0;
        Pattern p = Pattern.compile("PT(?:([0-9]+)H)?(?:([0-9]+)M)?(?:([0-9]+)S)?");
        Matcher m = p.matcher(iso8601);
        if (!m.find()) return 0;
        int hours = m.group(1) != null ? Integer.parseInt(m.group(1)) : 0;
        int minutes = m.group(2) != null ? Integer.parseInt(m.group(2)) : 0;
        int seconds = m.group(3) != null ? Integer.parseInt(m.group(3)) : 0;
        return hours * 60 + minutes + (seconds >= 30 ? 1 : 0);
    }

    public record PlaylistInfo(String playlistId, String title) {}
    public static class VideoInfo {
        public final String videoId;
        public final String title;
        public Integer durationMinutes;
        public VideoInfo(String videoId, String title, Integer durationMinutes) {
            this.videoId = videoId;
            this.title = title;
            this.durationMinutes = durationMinutes;
        }
    }
}
