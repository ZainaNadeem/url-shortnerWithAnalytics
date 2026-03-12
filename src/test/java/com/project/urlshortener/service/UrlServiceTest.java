package com.project.urlshortener.service;

import com.project.urlshortener.entity.Url;
import com.project.urlshortener.repository.UrlRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UrlServiceTest {

    @Mock
    private UrlRepository urlRepository;

    @InjectMocks
    private UrlService urlService;

    private Url testUrl;

    @BeforeEach
    void setUp() {
        testUrl = Url.builder()
                .id(1L)
                .shortCode("abc123")
                .originalUrl("https://www.google.com")
                .createdAt(LocalDateTime.now())
                .clickCount(0L)
                .build();
    }

    @Test
    @DisplayName("shortenUrl: saves and returns a Url with a generated short code")
    void shortenUrl_savesAndReturnsUrl() {
        when(urlRepository.existsByShortCode(anyString())).thenReturn(false);
        when(urlRepository.save(any(Url.class))).thenReturn(testUrl);

        Url result = urlService.shortenUrl("https://www.google.com", null);

        assertThat(result).isNotNull();
        assertThat(result.getOriginalUrl()).isEqualTo("https://www.google.com");
        verify(urlRepository, times(1)).save(any(Url.class));
    }

    @Test
    @DisplayName("shortenUrl: retries if short code already exists")
    void shortenUrl_retriesOnDuplicateShortCode() {
        when(urlRepository.existsByShortCode(anyString()))
                .thenReturn(true)
                .thenReturn(false);
        when(urlRepository.save(any(Url.class))).thenReturn(testUrl);

        Url result = urlService.shortenUrl("https://www.google.com", null);

        assertThat(result).isNotNull();
        verify(urlRepository, atLeast(2)).existsByShortCode(anyString());
    }

    @Test
    @DisplayName("shortenUrl: sets expiresAt when provided")
    void shortenUrl_setsExpiresAt() {
        LocalDateTime expiry = LocalDateTime.now().plusHours(1);
        Url urlWithExpiry = Url.builder()
                .id(2L)
                .shortCode("xyz789")
                .originalUrl("https://www.github.com")
                .createdAt(LocalDateTime.now())
                .expiresAt(expiry)
                .clickCount(0L)
                .build();

        when(urlRepository.existsByShortCode(anyString())).thenReturn(false);
        when(urlRepository.save(any(Url.class))).thenReturn(urlWithExpiry);

        Url result = urlService.shortenUrl("https://www.github.com", expiry);

        assertThat(result.getExpiresAt()).isEqualTo(expiry);
    }

    @Test
    @DisplayName("getByShortCode: returns Url when found")
    void getByShortCode_returnsUrl_whenFound() {
        when(urlRepository.findByShortCode("abc123")).thenReturn(Optional.of(testUrl));

        Url result = urlService.getByShortCode("abc123");

        assertThat(result).isNotNull();
        assertThat(result.getShortCode()).isEqualTo("abc123");
    }

    @Test
    @DisplayName("getByShortCode: returns null when not found")
    void getByShortCode_returnsNull_whenNotFound() {
        when(urlRepository.findByShortCode("notexist")).thenReturn(Optional.empty());

        Url result = urlService.getByShortCode("notexist");

        assertThat(result).isNull();
    }
}
