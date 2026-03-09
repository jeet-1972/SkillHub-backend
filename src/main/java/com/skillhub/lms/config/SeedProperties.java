package com.skillhub.lms.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "app.seed")
@Getter
@Setter
public class SeedProperties {

    private List<String> playlistIds = new ArrayList<>();
    private String defaultInstructorEmail = "instructor@skillhub.com";
}
