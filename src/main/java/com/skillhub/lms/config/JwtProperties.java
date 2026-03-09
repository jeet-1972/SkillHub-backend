package com.skillhub.lms.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.jwt")
@Getter
@Setter
public class JwtProperties {

    private String secret;
    /** Cookie name for legacy single-token (no longer used for access). */
    private String cookieName = "skillhub_token";
    /** Access token lifetime: 15 minutes. */
    private long accessTokenExpirationMs = 15 * 60 * 1000L;
    /** Refresh token lifetime: 30 days. */
    private long refreshTokenExpirationMs = 30 * 24 * 60 * 60 * 1000L;
    /** Cookie name for refresh token (HTTP-only). */
    private String refreshCookieName = "skillhub_refresh";
    @Deprecated
    private long expirationMs = 86400000;
}
