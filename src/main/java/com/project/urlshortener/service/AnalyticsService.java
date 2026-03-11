package com.project.urlshortener.service;

import com.project.urlshortener.entity.ClickEvent;
import com.project.urlshortener.entity.Url;
import com.project.urlshortener.repository.ClickEventRepository;
import com.project.urlshortener.repository.UrlRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final ClickEventRepository clickEventRepository;
    private final UrlRepository urlRepository;

    // Called asynchronously on every redirect — non-blocking
    @Async
    public void recordClick(Url url, String userAgent, String referer, String ipAddress) {
        ClickEvent event = ClickEvent.builder()
                .url(url)
                .clickedAt(LocalDateTime.now())
                .userAgent(userAgent)
                .referer(referer)
                .ipAddress(ipAddress)
                .build();
        clickEventRepository.save(event);

        // Also keep the denormalized click_count on the Url up to date
        urlRepository.findById(url.getId()).ifPresent(u -> {
            u.setClickCount(u.getClickCount() + 1);
            urlRepository.save(u);
        });
    }

    public Map<String, Object> getStats(String shortCode) {
        Optional<Url> urlOpt = urlRepository.findByShortCode(shortCode);
        if (urlOpt.isEmpty()) return Map.of("error", "URL not found");

        Url url = urlOpt.get();
        long totalClicks = clickEventRepository.countByUrlId(url.getId());
        long clicksToday = clickEventRepository.countByUrlIdSince(
                url.getId(), LocalDateTime.now().toLocalDate().atStartOfDay());
        long clicksThisWeek = clickEventRepository.countByUrlIdSince(
                url.getId(), LocalDateTime.now().minusDays(7));

        List<ClickEvent> recent = clickEventRepository
                .findByUrlIdOrderByClickedAtDesc(url.getId())
                .stream().limit(5).toList();

        return Map.of(
                "shortCode", shortCode,
                "originalUrl", url.getOriginalUrl(),
                "totalClicks", totalClicks,
                "clicksToday", clicksToday,
                "clicksThisWeek", clicksThisWeek,
                "createdAt", url.getCreatedAt().toString(),
                "recentClicks", recent.stream().map(e -> Map.of(
                        "clickedAt", e.getClickedAt().toString(),
                        "userAgent", e.getUserAgent() != null ? e.getUserAgent() : "unknown",
                        "referer", e.getReferer() != null ? e.getReferer() : "direct",
                        "ipAddress", e.getIpAddress() != null ? e.getIpAddress() : "unknown"
                )).toList()
        );
    }
}
