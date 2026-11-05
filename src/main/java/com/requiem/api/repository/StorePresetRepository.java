package com.requiem.api.repository;

import com.requiem.api.entity.StorePreset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StorePresetRepository extends JpaRepository<StorePreset, String> {

    List<StorePreset> findByUserIdOrderByCreatedAtDesc(String userId);

    Optional<StorePreset> findByIdAndUserId(String id, String userId);

    @Query("""
        SELECT p FROM StorePreset p
        WHERE (:search IS NULL OR :search = ''
               OR LOWER(p.title) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(p.tags) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(p.authorName) LIKE LOWER(CONCAT('%', :search, '%')))
          AND (:genre IS NULL OR :genre = '' OR LOWER(p.genre) = LOWER(:genre))
        ORDER BY p.createdAt DESC
    """)
    List<StorePreset> searchByRecent(@Param("search") String search, @Param("genre") String genre);

    @Query("""
        SELECT p FROM StorePreset p
        WHERE (:search IS NULL OR :search = ''
               OR LOWER(p.title) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(p.tags) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(p.authorName) LIKE LOWER(CONCAT('%', :search, '%')))
          AND (:genre IS NULL OR :genre = '' OR LOWER(p.genre) = LOWER(:genre))
        ORDER BY p.downloadsCount DESC, p.createdAt DESC
    """)
    List<StorePreset> searchByPopular(@Param("search") String search, @Param("genre") String genre);
}
