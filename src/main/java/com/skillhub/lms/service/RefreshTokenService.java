package com.skillhub.lms.service;

import com.skillhub.lms.entity.RefreshToken;
import com.skillhub.lms.entity.User;
import com.skillhub.lms.config.JwtProperties;
import com.skillhub.lms.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;

    @Transactional
    public String createAndSave(User user) {
        String rawToken = UUID.randomUUID().toString();
        String tokenHash = hash(rawToken);
        Instant expiresAt = Instant.now().plusMillis(jwtProperties.getRefreshTokenExpirationMs());
        RefreshToken rt = RefreshToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(expiresAt)
                .build();
        refreshTokenRepository.save(rt);
        return rawToken;
    }

    @Transactional(readOnly = true)
    public Optional<RefreshToken> validate(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) return Optional.empty();
        String hash = hash(rawToken);
        return refreshTokenRepository.findByTokenHash(hash)
                .filter(rt -> !rt.isRevoked() && !rt.isExpired());
    }

    @Transactional
    public void revokeByToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) return;
        String hash = hash(rawToken);
        refreshTokenRepository.findByTokenHash(hash)
                .ifPresent(rt -> {
                    rt.setRevokedAt(Instant.now());
                    refreshTokenRepository.save(rt);
                });
    }

    private static String hash(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
