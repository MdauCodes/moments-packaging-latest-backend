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

    private final Map<String, Bucket> loginBuckets       = new ConcurrentHashMap<>();
    private final Map<String, Bucket> leadBuckets        = new ConcurrentHashMap<>();
    private final Map<String, Bucket> enquiryBuckets     = new ConcurrentHashMap<>();
    private final Map<String, Bucket> clickBuckets       = new ConcurrentHashMap<>();
    private final Map<String, Bucket> cartBuckets        = new ConcurrentHashMap<>();
    private final Map<String, Bucket> checkoutBuckets    = new ConcurrentHashMap<>();
    private final Map<String, Bucket> paymentBuckets     = new ConcurrentHashMap<>();
    private final Map<String, Bucket> emailLookupBuckets = new ConcurrentHashMap<>();

    private Bucket newBucket(int capacity, int refillMinutes) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(capacity)
                .refillGreedy(capacity, Duration.ofMinutes(refillMinutes))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank())
            return forwarded.split(",")[0].trim();
        return request.getRemoteAddr();
    }

    public void checkLogin(HttpServletRequest request) {
        String ip = getClientIp(request);
        if (!loginBuckets.computeIfAbsent(ip, k -> newBucket(10, 1)).tryConsume(1))
            throw new RateLimitException("Too many login attempts. Please try again in a minute.");
    }

    public void checkLead(HttpServletRequest request) {
        String ip = getClientIp(request);
        if (!leadBuckets.computeIfAbsent(ip, k -> newBucket(20, 1)).tryConsume(1))
            throw new RateLimitException("Too many requests. Please try again in a minute.");
    }

    public void checkEnquiry(HttpServletRequest request) {
        String ip = getClientIp(request);
        if (!enquiryBuckets.computeIfAbsent(ip, k -> newBucket(10, 1)).tryConsume(1))
            throw new RateLimitException("Too many enquiries. Please try again in a minute.");
    }

    public void checkClick(HttpServletRequest request) {
        String ip = getClientIp(request);
        if (!clickBuckets.computeIfAbsent(ip, k -> newBucket(60, 1)).tryConsume(1))
            throw new RateLimitException("Too many requests.");
    }

    public void checkCart(HttpServletRequest request) {
        String ip = getClientIp(request);
        if (!cartBuckets.computeIfAbsent(ip, k -> newBucket(30, 1)).tryConsume(1))
            throw new RateLimitException("Too many cart requests. Please slow down.");
    }

    public void checkCheckout(HttpServletRequest request) {
        String ip = getClientIp(request);
        if (!checkoutBuckets.computeIfAbsent(ip, k -> newBucket(5, 1)).tryConsume(1))
            throw new RateLimitException("Too many checkout attempts. Please try again in a minute.");
    }

    public void checkPayment(HttpServletRequest request) {
        String ip = getClientIp(request);
        if (!paymentBuckets.computeIfAbsent(ip, k -> newBucket(3, 1)).tryConsume(1))
            throw new RateLimitException("Too many payment attempts. Please try again in a minute.");
    }

    /**
     * Rate limit for public email-based order lookup.
     * 10 requests per minute per IP — prevents email enumeration.
     */
    public void checkEmailLookup(HttpServletRequest request) {
        String ip = getClientIp(request);
        if (!emailLookupBuckets.computeIfAbsent(ip, k -> newBucket(10, 1)).tryConsume(1))
            throw new RateLimitException("Too many lookup attempts. Please try again in a minute.");
    }
}