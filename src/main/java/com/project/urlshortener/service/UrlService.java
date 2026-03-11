package com.project.urlshortener.service;

import com.project.urlshortener.entity.Url;
import com.project.urlshortener.repository.UrlRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UrlService {

    private final UrlRepository urlRepository;
    private static final String BASE62 = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int CODE_LENGTH = 6;
    private static final SecureRandom random = new SecureRandom();

    private String generateShortCode() {
        String code;
        do {
            StringBuilder sb = new StringBuilder(CODE_LENGTH);
            for (int i = 0; i < CODE_LENGTH; i++)
                sb.append(BASE62.charAt(random.nextInt(BASE62.length())));
            code = sb.toString();
        } while (urlRepository.existsByShortCode(code));
        return code;
    }

    public Url shortenUrl(String originalUrl, LocalDateTime expiresAt) {
        Url url = Url.builder()
                .originalUrl(originalUrl)
                .shortCode(generateShortCode())
                .expiresAt(expiresAt)
                .build();
        return urlRepository.save(url);
    }

    @Cacheable(value = "urls", key = "#shortCode")
    public Url getByShortCode(String shortCode) {
        System.out.println(">>> CACHE MISS — hitting MySQL for: " + shortCode);
        return urlRepository.findByShortCode(shortCode).orElse(null);
    }
}
