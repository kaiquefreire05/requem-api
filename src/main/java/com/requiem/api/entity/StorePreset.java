package com.requiem.api.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "store_presets")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StorePreset {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String genre;

    private String tags;

    private Integer bpm;

    private String tonality;

    @Column(name = "time_signature")
    private String timeSignature;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "configuration_data", columnDefinition = "jsonb", nullable = false)
    private JsonNode configurationData;

    @Column(name = "downloads_count", nullable = false)
    @Builder.Default
    private Integer downloadsCount = 0;

    @Column(name = "likes_count", nullable = false)
    @Builder.Default
    private Integer likesCount = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "author_name", nullable = false)
    private String authorName;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        if (downloadsCount == null) downloadsCount = 0;
        if (likesCount == null) likesCount = 0;
        createdAt = updatedAt = Instant.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
