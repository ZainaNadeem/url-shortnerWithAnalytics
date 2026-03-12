package com.project.urlshortener.service;

import com.project.urlshortener.entity.ClickEvent;
import com.project.urlshortener.entity.Url;
import com.project.urlshortener.repository.ClickEventRepository;
import com.project.urlshortener.repository.UrlRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private ClickEventRepository clickEventRepository;

    @Mock
    private UrlRepository urlRepository;

    @InjectMocks
    private AnalyticsService analyticsService;

    private Url testUrl;

    @BeforeEach
    void setUp() {
        testUrl = Url.builder()
                .id(1L)
                .shortCode("abc123")
                .originalUrl("https://www.google.com")
                .createdAt(LocalDateTime.now())
                .clickCount(5L)
                .build();
    }

    @Test
    @DisplayName("recordClick: saves a ClickEvent and increments clickCount")
    void recordClick_savesClickEventAndIncrementsCount() {
        when(urlRepository.findById(1L)).thenReturn(Optional.of(testUrl));

        analyticsService.recordClick(testUrl, "Mozilla/5.0", "https://referrer.com", "127.0.0.1");

        verify(clickEventRepository, times(1)).save(any(ClickEvent.class));
        verify(urlRepository, times(1)).save(any(Url.class));
        assertThat(testUrl.getClickCount()).isEqualTo(6L);
    }

    @Test
    @DisplayName("getStats: returns correct stats map for existing shortCode")
    void getStats_returnsCorrectStats() {
        ClickEvent event = ClickEvent.builder()
                .id(1L)
                .url(testUrl)
                .clickedAt(LocalDateTime.now())
                .userAgent("curl/8.7.1")
                .ipAddress("127.0.0.1")
                .build();

        when(urlRepository.findByShortCode("abc123")).thenReturn(Optional.of(testUrl));
        when(clickEventRepository.countByUrlId(1L)).thenReturn(5L);
        when(clickEventRepository.countByUrlIdSince(eq(1L), any())).thenReturn(3L);
        when(clickEventRepository.findByUrlIdOrderByClickedAtDesc(1L)).thenReturn(List.of(event));

        Map<String, Object> stats = analyticsService.getStats("abc123");

        assertThat(stats).containsKey("totalClicks");
        assertThat(stats.get("totalClicks")).isEqualTo(5L);
        assertThat(stats.get("shortCode")).isEqualTo("abc123");
        assertThat(stats.get("originalUrl")).isEqualTo("https://www.google.com");
    }

    @Test
    @DisplayName("getStats: returns error map when shortCode not found")
    void getStats_returnsError_whenNotFound() {
        when(urlRepository.findByShortCode("notexist")).thenReturn(Optional.empty());

        Map<String, Object> stats = analyticsService.getStats("notexist");

        assertThat(stats).containsKey("error");
    }
}
