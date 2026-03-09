package com.skillhub.lms.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "video_progress", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "video_id"})
}, indexes = {
    @Index(name = "idx_video_progress_user", columnList = "user_id"),
    @Index(name = "idx_video_progress_video", columnList = "video_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id", nullable = false)
    private Video video;

    @Column(name = "last_position_seconds", nullable = false)
    @Builder.Default
    private Integer lastPositionSeconds = 0;

    @Column(name = "is_completed", nullable = false)
    @Builder.Default
    private Boolean isCompleted = false;

    @Column(name = "completed_at")
    private Instant completedAt;

    @PrePersist
    @PreUpdate
    protected void onComplete() {
        if (Boolean.TRUE.equals(isCompleted) && completedAt == null) {
            completedAt = Instant.now();
        }
    }
}
