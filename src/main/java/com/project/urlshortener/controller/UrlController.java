package com.project.urlshortener.controller;

import com.project.urlshortener.entity.Url;
import com.project.urlshortener.service.AnalyticsService;
import com.project.urlshortener.service.ExpirationService;
import com.project.urlshortener.service.UrlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Tag(name = "URL Shortener", description = "Shorten URLs, redirect, and view analytics")
public class UrlController {

    private final UrlService urlService;
    private final AnalyticsService analyticsService;
    private final ExpirationService expirationService;

    @Operation(summary = "Shorten a URL",
               description = "Creates a short code for the given URL. Optionally set expiry in minutes.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "URL shortened successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request body")
    })
    @PostMapping("/shorten")
    public ResponseEntity<Map<String, Object>> shortenUrl(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "URL to shorten. Use 'expiresInMinutes' to set TTL.",
                required = true)
            @RequestBody Map<String, String> body) {

        String originalUrl = body.get("url");
        if (originalUrl == null || originalUrl.isBlank())
            return ResponseEntity.badRequest().body(Map.of("error", "URL is required"));

        LocalDateTime expiresAt = null;
        String expiresInMinutes = body.get("expiresInMinutes");
        if (expiresInMinutes != null) {
            try {
                expiresAt = LocalDateTime.now().plusMinutes(Long.parseLong(expiresInMinutes));
            } catch (NumberFormatException e) {
                return ResponseEntity.badRequest().body(Map.of("error", "expiresInMinutes must be a number"));
            }
        }

        Url saved = urlService.shortenUrl(originalUrl, expiresAt);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("shortCode", saved.getShortCode());
        response.put("shortUrl", "http://localhost:8081/" + saved.getShortCode());
        response.put("originalUrl", saved.getOriginalUrl());
        response.put("createdAt", saved.getCreatedAt().toString());
        response.put("expiresAt", saved.getExpiresAt() != null ? saved.getExpiresAt().toString() : "never");
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Redirect to original URL",
               description = "Looks up the short code and redirects. Returns 410 if expired, 404 if not found.")
    @ApiResponses({
        @ApiResponse(responseCode = "302", description = "Redirect to original URL"),
        @ApiResponse(responseCode = "404", description = "Short code not found"),
        @ApiResponse(responseCode = "410", description = "URL has expired")
    })
    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirect(
            @Parameter(description = "The short code to redirect") @PathVariable String shortCode,
            HttpServletRequest request) {

        Url url = urlService.getByShortCode(shortCode);
        if (url == null) return ResponseEntity.notFound().build();
        if (url.getExpiresAt() != null && url.getExpiresAt().isBefore(LocalDateTime.now()))
            return ResponseEntity.status(HttpStatus.GONE).build();

        analyticsService.recordClick(url,
                request.getHeader("User-Agent"),
                request.getHeader("Referer"),
                request.getRemoteAddr());

        return ResponseEntity.status(HttpStatus.FOUND)
                .header("Location", url.getOriginalUrl())
                .build();
    }

    @Operation(summary = "Get click analytics",
               description = "Returns total clicks, clicks today, clicks this week, and recent click history.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Analytics returned"),
        @ApiResponse(responseCode = "404", description = "Short code not found")
    })
    @GetMapping("/stats/{shortCode}")
    public ResponseEntity<Map<String, Object>> getStats(
            @Parameter(description = "The short code to get stats for") @PathVariable String shortCode) {
        return ResponseEntity.ok(analyticsService.getStats(shortCode));
    }

    @Operation(summary = "Trigger expired URL cleanup",
               description = "Manually runs the expiration cleanup job. Deletes expired URLs from DB and Redis.")
    @ApiResponse(responseCode = "200", description = "Cleanup triggered successfully")
    @PostMapping("/admin/cleanup")
    public ResponseEntity<String> triggerCleanup() {
        expirationService.cleanupExpiredUrls();
        return ResponseEntity.ok("Cleanup triggered");
    }
}
