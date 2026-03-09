package com.skillhub.lms.seed;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Profile("seed")
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class YouTubeSeedRunner implements CommandLineRunner {

    private final YouTubeSeedService youTubeSeedService;

    @Override
    public void run(String... args) {
        log.info("Running YouTube seed (seed profile active)...");
        youTubeSeedService.runSeed();
    }
}
