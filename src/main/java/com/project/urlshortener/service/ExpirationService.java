package com.project.urlshortener.service;

import com.project.urlshortener.entity.Url;
import com.project.urlshortener.repository.ClickEventRepository;
import com.project.urlshortener.repository.UrlRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExpirationService {

    private final UrlRepository urlRepository;
    private final ClickEventRepository clickEventRepository;
    private final CacheManager cacheManager;

    // Runs every hour at the top of the hour
    // cron = "second minute hour day month weekday"
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void cleanupExpiredUrls() {
        List<Url> expired = urlRepository.findAllExpiredBefore(LocalDateTime.now());

        if (expired.isEmpty()) {
            log.info("Expiration cleanup: no expired URLs found");
            return;
        }

        log.info("Expiration cleanup: found {} expired URLs to delete", expired.size());

        for (Url url : expired) {
            // 1. Delete all click events for this URL (child records first)
            clickEventRepository.deleteAll(
                clickEventRepository.findByUrlIdOrderByClickedAtDesc(url.getId())
            );

            // 2. Evict from Redis cache
            var cache = cacheManager.getCache("urls");
            if (cache != null) {
                cache.evict(url.getShortCode());
                log.info("Evicted {} from Redis cache", url.getShortCode());
            }

            // 3. Delete the URL from MySQL
            urlRepository.delete(url);
            log.info("Deleted expired URL: {} -> {}", url.getShortCode(), url.getOriginalUrl());
        }

        log.info("Expiration cleanup complete: deleted {} URLs", expired.size());
    }
}
