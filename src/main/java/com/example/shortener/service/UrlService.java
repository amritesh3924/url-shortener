package com.example.shortener.service;

import com.example.shortener.model.Click;
import com.example.shortener.model.Url;
import com.example.shortener.repository.ClickRepository;
import com.example.shortener.repository.UrlRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Service
public class UrlService {
    private static final Pattern ALIAS_PATTERN = Pattern.compile("[A-Za-z0-9-]{3,20}");
    private final UrlRepository urls;
    private final ClickRepository clicks;
    private final Base62Encoder encoder;
    private final UrlValidator validator;
    private final StringRedisTemplate redis;
    private final String baseUrl;
    private final boolean cacheEnabled;

    public UrlService(UrlRepository urls, ClickRepository clicks, Base62Encoder encoder,
                      UrlValidator validator, StringRedisTemplate redis,
                      @Value("${app.base-url}") String baseUrl,
                      @Value("${app.cache-enabled:true}") boolean cacheEnabled) {
        this.urls = urls;
        this.clicks = clicks;
        this.encoder = encoder;
        this.validator = validator;
        this.redis = redis;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.cacheEnabled = cacheEnabled;
    }

    @Transactional
    public ShortenResult shorten(String longUrl, String customAlias, Integer expiryDays) {
        if (!validator.isValid(longUrl)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "URL must be a valid http or https URL up to 2048 characters");
        if (expiryDays != null && expiryDays < 1) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "expiryDays must be positive");
        if (customAlias != null && !customAlias.isBlank()) {
            if (!ALIAS_PATTERN.matcher(customAlias).matches()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "customAlias must be 3-20 letters, numbers, or hyphens");
            if (urls.existsByShortCode(customAlias)) throw new ResponseStatusException(HttpStatus.CONFLICT, "That alias is already in use");
        }
        Instant expiresAt = expiryDays == null ? null : Instant.now().plus(expiryDays, ChronoUnit.DAYS);
        Url url = urls.save(new Url(longUrl, expiresAt));
        url.setShortCode(customAlias == null || customAlias.isBlank() ? encoder.encode(url.getId()) : customAlias);
        urls.save(url);
        return new ShortenResult(url.getShortCode(), baseUrl + "/" + url.getShortCode(), expiresAt);
    }

    @Transactional
    public String resolveAndTrack(String code, String referrer, String userAgent) {
        String longUrl;
        String cacheKey = "url:" + code;
        String cached = cacheEnabled ? redis.opsForValue().get(cacheKey) : null;
        if (cached != null) {
            incrementCounter("stats:cache:hits");
            String[] fields = cached.split("\\n", 2);
            longUrl = fields[1];
            if (!fields[0].isEmpty() && Instant.ofEpochMilli(Long.parseLong(fields[0])).isBefore(Instant.now())) {
                redis.delete(cacheKey);
                throw new ResponseStatusException(HttpStatus.GONE, "This link has expired");
            }
        } else {
            incrementCounter("stats:cache:misses");
            Url url = urls.findByShortCode(code).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Short link not found"));
            if (url.getExpiresAt() != null && url.getExpiresAt().isBefore(Instant.now())) {
                throw new ResponseStatusException(HttpStatus.GONE, "This link has expired");
            }
            longUrl = url.getLongUrl();
            if (cacheEnabled) {
                String expiry = url.getExpiresAt() == null ? "" : Long.toString(url.getExpiresAt().toEpochMilli());
                redis.opsForValue().set(cacheKey, expiry + "\n" + longUrl, 1, TimeUnit.HOURS);
            }
        }
        urls.incrementClicks(code);
        clicks.save(new Click(code, trim(referrer, 500), deviceType(userAgent)));
        return longUrl;
    }

    @Transactional(readOnly = true)
    public UrlStats getStats(String code) {
        Url url = urls.findByShortCode(code).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Short link not found"));
        Map<String, Long> perDay = new TreeMap<>();
        Map<String, Long> referrers = new HashMap<>();
        Map<String, Long> devices = new TreeMap<>();
        for (Click click : clicks.findAllByShortCode(code)) {
            Instant time = click.getClickedAt();
            String day = time.toString().substring(0, 10);
            perDay.merge(day, 1L, Long::sum);
            if (click.getReferrer() != null) referrers.merge(click.getReferrer(), 1L, Long::sum);
            devices.merge(click.getDeviceType(), 1L, Long::sum);
        }
        Map<String, Long> topReferrers = new LinkedHashMap<>();
        referrers.entrySet().stream().sorted(Map.Entry.<String, Long>comparingByValue().reversed()).limit(5)
                .forEach(entry -> topReferrers.put(entry.getKey(), entry.getValue()));
        return new UrlStats(url.getLongUrl(), url.getClickCount(), perDay, topReferrers, devices);
    }

    public CacheStats getCacheStats() {
        long hits = counterValue("stats:cache:hits");
        long misses = counterValue("stats:cache:misses");
        long total = hits + misses;
        double rate = total == 0 ? 0 : Math.round((double) hits * 10000 / total) / 100.0;
        return new CacheStats(hits, misses, rate);
    }

    private void incrementCounter(String key) { redis.opsForValue().increment(key); }
    private long counterValue(String key) {
        String value = redis.opsForValue().get(key);
        return value == null ? 0 : Long.parseLong(value);
    }
    private String trim(String value, int maxLength) { return value == null ? null : value.substring(0, Math.min(value.length(), maxLength)); }
    private String deviceType(String agent) {
        if (agent == null) return "other";
        String lower = agent.toLowerCase(Locale.ROOT);
        if (lower.contains("mobile") || lower.contains("android") || lower.contains("iphone")) return "mobile";
        if (lower.contains("mozilla") || lower.contains("chrome") || lower.contains("safari")) return "desktop";
        return "other";
    }

    public record ShortenResult(String shortCode, String shortUrl, Instant expiresAt) { }
    public record UrlStats(String longUrl, long totalClicks, Map<String, Long> clicksPerDay,
                           Map<String, Long> topReferrers, Map<String, Long> deviceBreakdown) { }
    public record CacheStats(long cacheHits, long cacheMisses, double hitRatePercent) { }
}
