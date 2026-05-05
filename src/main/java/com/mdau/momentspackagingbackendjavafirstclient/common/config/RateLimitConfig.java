package com.mdau.momentspackagingbackendjavafirstclient.common.config;

import com.mdau.momentspackagingbackendjavafirstclient.common.exception.RateLimitException;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitConfig {

    private final Map<String, Bucket> loginBuckets     = new ConcurrentHashMap<>();
    private final Map<String, Bucket> leadBuckets      = new ConcurrentHashMap<>();
    private final Map<String, Bucket> enquiryBuckets   = new ConcurrentHashMap<>();
    private final Map<String, Bucket> clickBuckets     = new ConcurrentHashMap<>();

    private Bucket newBucket(int capacity, int refillMinutes) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(capacity)
                .refillGreedy(capacity, Duration.ofMinutes(refillMinutes))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    public void checkLogin(HttpServletRequest request) {
        String ip = getClientIp(request);
        Bucket bucket = loginBuckets.computeIfAbsent(ip, k -> newBucket(10, 1));
        if (!bucket.tryConsume(1)) {
            throw new RateLimitException("Too many login attempts. Please try again in a minute.");
        }
    }

    public void checkLead(HttpServletRequest request) {
        String ip = getClientIp(request);
        Bucket bucket = leadBuckets.computeIfAbsent(ip, k -> newBucket(20, 1));
        if (!bucket.tryConsume(1)) {
            throw new RateLimitException("Too many requests. Please try again in a minute.");
        }
    }

    public void checkEnquiry(HttpServletRequest request) {
        String ip = getClientIp(request);
        Bucket bucket = enquiryBuckets.computeIfAbsent(ip, k -> newBucket(10, 1));
        if (!bucket.tryConsume(1)) {
            throw new RateLimitException("Too many enquiries. Please try again in a minute.");
        }
    }

    public void checkClick(HttpServletRequest request) {
        String ip = getClientIp(request);
        Bucket bucket = clickBuckets.computeIfAbsent(ip, k -> newBucket(60, 1));
        if (!bucket.tryConsume(1)) {
            throw new RateLimitException("Too many requests.");
        }
    }
}