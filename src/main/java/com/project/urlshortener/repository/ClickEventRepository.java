package com.project.urlshortener.repository;

import com.project.urlshortener.entity.ClickEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ClickEventRepository extends JpaRepository<ClickEvent, Long> {

    // Count all clicks for a URL
    long countByUrlId(Long urlId);

    // Get all clicks for a URL ordered by most recent
    List<ClickEvent> findByUrlIdOrderByClickedAtDesc(Long urlId);

    // Count clicks within a time range (for "clicks today" etc)
    @Query("SELECT COUNT(c) FROM ClickEvent c WHERE c.url.id = :urlId AND c.clickedAt >= :since")
    long countByUrlIdSince(@Param("urlId") Long urlId, @Param("since") LocalDateTime since);
}
