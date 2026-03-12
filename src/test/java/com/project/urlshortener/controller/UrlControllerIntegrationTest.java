package com.project.urlshortener.controller;

import com.project.urlshortener.entity.Url;
import com.project.urlshortener.service.AnalyticsService;
import com.project.urlshortener.service.ExpirationService;
import com.project.urlshortener.service.UrlService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UrlController.class)
class UrlControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UrlService urlService;

    @MockitoBean
    private AnalyticsService analyticsService;

    @MockitoBean
    private ExpirationService expirationService;

    @Test
    @DisplayName("POST /shorten: returns 200 with shortCode for valid URL")
    void shorten_returns200_forValidUrl() throws Exception {
        Url mockUrl = Url.builder()
                .id(1L)
                .shortCode("abc123")
                .originalUrl("https://www.google.com")
                .createdAt(LocalDateTime.now())
                .clickCount(0L)
                .build();

        when(urlService.shortenUrl(anyString(), any())).thenReturn(mockUrl);

        mockMvc.perform(post("/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\": \"https://www.google.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortCode").value("abc123"))
                .andExpect(jsonPath("$.originalUrl").value("https://www.google.com"));
    }

    @Test
    @DisplayName("POST /shorten: returns 400 when URL is missing")
    void shorten_returns400_whenUrlMissing() throws Exception {
        mockMvc.perform(post("/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("URL is required"));
    }

    @Test
    @DisplayName("GET /{shortCode}: returns 302 for valid short code")
    void redirect_returns302_forValidShortCode() throws Exception {
        Url mockUrl = Url.builder()
                .id(1L)
                .shortCode("abc123")
                .originalUrl("https://www.google.com")
                .createdAt(LocalDateTime.now())
                .clickCount(0L)
                .build();

        when(urlService.getByShortCode("abc123")).thenReturn(mockUrl);

        mockMvc.perform(get("/abc123"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://www.google.com"));
    }

    @Test
    @DisplayName("GET /{shortCode}: returns 404 when short code not found")
    void redirect_returns404_whenNotFound() throws Exception {
        when(urlService.getByShortCode("notexist")).thenReturn(null);

        mockMvc.perform(get("/notexist"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /{shortCode}: returns 410 when URL is expired")
    void redirect_returns410_whenExpired() throws Exception {
        Url expiredUrl = Url.builder()
                .id(1L)
                .shortCode("expired")
                .originalUrl("https://www.google.com")
                .createdAt(LocalDateTime.now().minusDays(2))
                .expiresAt(LocalDateTime.now().minusHours(1))
                .clickCount(0L)
                .build();

        when(urlService.getByShortCode("expired")).thenReturn(expiredUrl);

        mockMvc.perform(get("/expired"))
                .andExpect(status().isGone());
    }

    @Test
    @DisplayName("GET /stats/{shortCode}: returns analytics map")
    void stats_returnsAnalytics() throws Exception {
        when(analyticsService.getStats("abc123")).thenReturn(Map.of(
                "shortCode", "abc123",
                "totalClicks", 5L,
                "clicksToday", 2L,
                "clicksThisWeek", 5L,
                "originalUrl", "https://www.google.com",
                "createdAt", LocalDateTime.now().toString(),
                "recentClicks", List.of()
        ));

        mockMvc.perform(get("/stats/abc123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortCode").value("abc123"))
                .andExpect(jsonPath("$.totalClicks").value(5));
    }

    @Test
    @DisplayName("POST /admin/cleanup: returns 200 and triggers cleanup")
    void cleanup_returns200() throws Exception {
        mockMvc.perform(post("/admin/cleanup"))
                .andExpect(status().isOk());

        verify(expirationService, times(1)).cleanupExpiredUrls();
    }
}
