package com.example.shortener.controller;

import com.example.shortener.service.RateLimiter;
import com.example.shortener.service.UrlService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.Map;

@RestController
@Validated
public class UrlController {
    private final UrlService urlService;
    private final RateLimiter rateLimiter;

    public UrlController(UrlService urlService, RateLimiter rateLimiter) {
        this.urlService = urlService;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/api/shorten")
    public UrlService.ShortenResult shorten(@Valid @RequestBody ShortenRequest body, HttpServletRequest request) {
        if (!rateLimiter.allow(request.getRemoteAddr())) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded; try again next minute");
        }
        return urlService.shorten(body.longUrl(), body.customAlias(), body.expiryDays());
    }

    @GetMapping("/{code:[A-Za-z0-9-]{1,20}}")
    public ResponseEntity<Void> redirect(@PathVariable String code, HttpServletRequest request) {
        String destination = urlService.resolveAndTrack(code, request.getHeader("Referer"), request.getHeader("User-Agent"));
        return ResponseEntity.status(HttpStatus.FOUND).header(HttpHeaders.LOCATION, destination).build();
    }

    @GetMapping("/api/stats/{code}")
    public UrlService.UrlStats stats(@PathVariable String code) { return urlService.getStats(code); }

    @GetMapping("/api/stats")
    public UrlService.CacheStats cacheStats() { return urlService.getCacheStats(); }

    @GetMapping(value = "/health", produces = "text/plain")
    public String health() { return "OK"; }

    public record ShortenRequest(@NotBlank @Size(max = 2048) String longUrl,
                                 @Size(max = 20) String customAlias,
                                 @Min(1) @Max(36500) Integer expiryDays) { }
}
